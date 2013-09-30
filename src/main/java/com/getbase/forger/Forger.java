/*
 * Copyright (C) 2013 Jerzy Chalupski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.getbase.forger;

import com.getbase.forger.thneed.ContentResolverModel;
import com.getbase.forger.thneed.MicroOrmModel;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import org.chalup.microorm.MicroOrm;
import org.chalup.microorm.annotations.Column;
import org.chalup.microorm.annotations.Embedded;
import org.chalup.thneed.ManyToManyRelationship;
import org.chalup.thneed.ModelGraph;
import org.chalup.thneed.ModelVisitor;
import org.chalup.thneed.OneToManyRelationship;
import org.chalup.thneed.OneToOneRelationship;
import org.chalup.thneed.PolymorphicRelationship;
import org.chalup.thneed.RecursiveModelRelationship;
import org.chalup.thneed.RelationshipVisitor;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Forger<TModel extends ContentResolverModel & MicroOrmModel> {

  public static <TModel extends ContentResolverModel & MicroOrmModel> Builder<TModel> builder() {
    return new Builder<TModel>();
  }

  public static class Builder<TModel extends ContentResolverModel & MicroOrmModel> {

    private ModelGraph<TModel> mModelGraph;
    private MicroOrm mMicroOrm;
    private Map<Class<?>, FakeDataGenerator<?>> mCustomGenerators = Maps.newLinkedHashMap();

    private Builder() {
    }

    public Builder<TModel> withModelGraph(ModelGraph<TModel> modelGraph) {
      Preconditions.checkState(mModelGraph == null, "You've already set ModelGraph");
      mModelGraph = modelGraph;
      return this;
    }

    public Builder<TModel> withMicroOrm(MicroOrm microOrm) {
      Preconditions.checkState(mMicroOrm == null, "You've already set ModelGraph");
      mMicroOrm = microOrm;
      return this;
    }

    public <Type> Builder<TModel> registerCustomGenerator(Class<Type> clazz, FakeDataGenerator<Type> generator) {
      Preconditions.checkArgument(clazz != null, "Passed class can't be null");
      Preconditions.checkArgument(generator != null, "Passed generator can't be null");
      Preconditions.checkState(!mCustomGenerators.containsKey(clazz),
          String.format("You've already registered generator for %s", clazz.getSimpleName()));
      mCustomGenerators.put(clazz, generator);
      return this;
    }

    public Forger<TModel> build() {
      Preconditions.checkState(mModelGraph != null, "ModelGraph is not set");
      Preconditions.checkState(mMicroOrm != null, "MicroOrm is not set");
      final Map<Class<?>, FakeDataGenerator<?>> filteredDefaults =
          Maps.filterKeys(getDefaultGenerators(), new Predicate<Class<?>>() {
            @Override
            public boolean apply(java.lang.Class<?> clazz) {
              return !mCustomGenerators.containsKey(clazz);
            }
          });
      final ImmutableMap<Class<?>, FakeDataGenerator<?>> generators =
          ImmutableMap.<Class<?>, FakeDataGenerator<?>>builder()
              .putAll(mCustomGenerators)
              .putAll(filteredDefaults)
              .build();
      return new Forger<TModel>(mModelGraph, mMicroOrm, generators);
    }
  }

  private final Map<Class<?>, TModel> mModels;
  private final MicroOrm mMicroOrm;
  private final Map<Class<?>, FakeDataGenerator<?>> mGenerators;
  private final Multimap<Class<?>, Dependency<TModel>> mDependencies;
  private final Map<IdColumnKey, IdGetter> mIdGetters;
  private final Map<Class<?>, Object> mContext;

  private Forger(Forger<TModel> forger, Map<Class<?>, Object> context) {
    mModels = forger.mModels;
    mMicroOrm = forger.mMicroOrm;
    mGenerators = forger.mGenerators;
    mDependencies = forger.mDependencies;
    mIdGetters = forger.mIdGetters;

    mContext = context;
  }

  private interface Dependency<T extends ContentResolverModel & MicroOrmModel> {
    boolean canBeSatisfiedWith(Class<?> klass);

    Collection<String> getColumns();

    void satisfyDependencyWith(ContentValues contentValues, Object o);

    void satisfyDependencyWithNewObject(ContentValues contentValues, Forger<T> forger, ContentResolver resolver);
  }

  public Forger(ModelGraph<TModel> modelGraph, MicroOrm microOrm) {
    this(modelGraph, microOrm, getDefaultGenerators());
  }

  private static class IdColumnKey {
    private final Class<?> mClass;
    private final String mColumn;

    private IdColumnKey(Class<?> klass, String column) {
      Preconditions.checkNotNull(klass);
      Preconditions.checkNotNull(column);

      mClass = klass;
      mColumn = column;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      IdColumnKey that = (IdColumnKey) o;

      if (!mClass.equals(that.mClass)) return false;
      if (!mColumn.equals(that.mColumn)) return false;

      return true;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(mClass, mColumn);
    }
  }

  private interface IdGetter {
    Object getId(Object o);
  }

  private IdGetter createIdGetter(final IdColumnKey idColumnKey) {
    for (final Field field : Fields.allFieldsIncludingPrivateAndSuper(idColumnKey.mClass)) {

      Column columnAnnotation = field.getAnnotation(Column.class);
      if (columnAnnotation != null && columnAnnotation.value().equals(idColumnKey.mColumn)) {
        return new IdGetter() {

          @Override
          public Object getId(Object o) {
            boolean wasAccessible = field.isAccessible();
            try {
              field.setAccessible(true);
              return field.get(o);
            } catch (IllegalAccessException e) {
              throw new IllegalArgumentException("Forger cannot access " + idColumnKey.mColumn + " column in " + o, e);
            } finally {
              field.setAccessible(wasAccessible);
            }
          }
        };
      }
    }

    throw new IllegalArgumentException("Forger cannot create id getter in " + idColumnKey.mClass + ". Make sure that this class has a field annotated with @Column('" + idColumnKey.mColumn + "').");
  }

  private Forger(ModelGraph<TModel> modelGraph, MicroOrm microOrm, Map<Class<?>, FakeDataGenerator<?>> generators) {
    mModels = Maps.newHashMap();
    mMicroOrm = microOrm;
    mGenerators = generators;
    mDependencies = HashMultimap.create();
    mIdGetters = Maps.newHashMap();
    mContext = Maps.newLinkedHashMap();

    modelGraph.accept(new ModelVisitor<TModel>() {
      @Override
      public void visit(TModel model) {
        Class<?> modelClass = model.getModelClass();

        mModels.put(modelClass, model);
      }
    });

    modelGraph.accept(new RelationshipVisitor<TModel>() {
      private Object getId(Object o, String idColumn) {
        IdColumnKey idColumnKey = new IdColumnKey(o.getClass(), idColumn);

        final IdGetter idGetter;
        if (mIdGetters.containsKey(idColumnKey)) {
          idGetter = mIdGetters.get(idColumnKey);
        } else {
          idGetter = createIdGetter(idColumnKey);
          mIdGetters.put(idColumnKey, idGetter);
        }
        return idGetter.getId(o);
      }

      @Override
      public void visit(final OneToManyRelationship<? extends TModel> relationship) {
        TModel model = relationship.mModel;
        mDependencies.put(model.getModelClass(), new Dependency<TModel>() {
          @Override
          public boolean canBeSatisfiedWith(Class<?> klass) {
            TModel parentModel = relationship.mReferencedModel;
            return parentModel.getModelClass().equals(klass);
          }

          @Override
          public Collection<String> getColumns() {
            return Lists.newArrayList(relationship.mLinkedByColumn);
          }

          @Override
          public void satisfyDependencyWith(ContentValues contentValues, Object o) {
            putIntoContentValues(contentValues, relationship.mLinkedByColumn, getId(o, relationship.mReferencedModelIdColumn));
          }

          @Override
          public void satisfyDependencyWithNewObject(ContentValues contentValues, Forger<TModel> forger, ContentResolver resolver) {
            TModel referencedModel = relationship.mReferencedModel;
            Class<?> modelClass = referencedModel.getModelClass();

            if (forger.mContext.containsKey(modelClass)) {
              satisfyDependencyWith(contentValues, forger.mContext.get(modelClass));
            } else {
              satisfyDependencyWith(contentValues, forger.iNeed(modelClass).in(resolver));
            }
          }
        });
      }

      @Override
      public void visit(final OneToOneRelationship<? extends TModel> relationship) {
        TModel linkedModel = relationship.mLinkedModel;
        mDependencies.put(linkedModel.getModelClass(), new Dependency<TModel>() {
          @Override
          public boolean canBeSatisfiedWith(Class<?> klass) {
            TModel model = relationship.mModel;
            return model.getModelClass().equals(klass);
          }

          @Override
          public Collection<String> getColumns() {
            return Lists.newArrayList(relationship.mLinkedByColumn);
          }

          @Override
          public void satisfyDependencyWith(ContentValues contentValues, Object o) {
            putIntoContentValues(contentValues, relationship.mLinkedByColumn, getId(o, relationship.mParentModelIdColumn));
          }

          @Override
          public void satisfyDependencyWithNewObject(ContentValues contentValues, Forger<TModel> forger, ContentResolver resolver) {
            TModel model = relationship.mModel;
            Class<?> modelClass = model.getModelClass();

            if (forger.mContext.containsKey(modelClass)) {
              satisfyDependencyWith(contentValues, forger.mContext.get(modelClass));
            } else {
              satisfyDependencyWith(contentValues, forger.iNeed(modelClass).in(resolver));
            }
          }
        });
      }

      @Override
      public void visit(final RecursiveModelRelationship<? extends TModel> relationship) {
        TModel model = relationship.mModel;
        mDependencies.put(model.getModelClass(), new Dependency<TModel>() {
          @Override
          public boolean canBeSatisfiedWith(Class<?> klass) {
            TModel model = relationship.mModel;
            return model.getModelClass().equals(klass);
          }

          @Override
          public Collection<String> getColumns() {
            return Lists.newArrayList(relationship.mGroupByColumn);
          }

          @Override
          public void satisfyDependencyWith(ContentValues contentValues, Object o) {
            putIntoContentValues(contentValues, relationship.mGroupByColumn, getId(o, relationship.mModelIdColumn));
          }

          @Override
          public void satisfyDependencyWithNewObject(ContentValues contentValues, Forger<TModel> forger, ContentResolver resolver) {
            TModel model = relationship.mModel;
            Class<?> modelClass = model.getModelClass();

            if (forger.mContext.containsKey(modelClass)) {
              satisfyDependencyWith(contentValues, forger.mContext.get(modelClass));
            } else {
              contentValues.putNull(relationship.mGroupByColumn);
            }
          }
        });
      }

      @Override
      public void visit(final ManyToManyRelationship<? extends TModel> relationship) {
        // no implementation needed, both sides of relationship will be visited
      }

      @Override
      public void visit(final PolymorphicRelationship<? extends TModel> relationship) {
        TModel model = relationship.mModel;
        mDependencies.put(model.getModelClass(), new Dependency<TModel>() {
          @Override
          public boolean canBeSatisfiedWith(Class<?> klass) {
            for (TModel model : relationship.mPolymorphicModels.values()) {
              if (model.getModelClass().equals(klass)) {
                return true;
              }
            }

            return false;
          }

          @Override
          public Collection<String> getColumns() {
            return Lists.newArrayList(relationship.mTypeColumnName, relationship.mIdColumnName);
          }

          @Override
          public void satisfyDependencyWith(ContentValues contentValues, Object o) {
            for (Map.Entry<String, ? extends TModel> polymorphicType : relationship.mPolymorphicModels.entrySet()) {
              TModel model = polymorphicType.getValue();
              if (model.getModelClass().equals(o.getClass())) {
                contentValues.put(relationship.mTypeColumnName, polymorphicType.getKey());
                putIntoContentValues(contentValues, relationship.mIdColumnName, getId(o, relationship.mPolymorphicModelIdColumn));
                return;
              }
            }

            throw new IllegalStateException();
          }

          @Override
          public void satisfyDependencyWithNewObject(ContentValues contentValues, Forger<TModel> forger, ContentResolver resolver) {
            for (Class<?> klass : Lists.reverse(Lists.newArrayList(forger.mContext.keySet()))) {
              if (canBeSatisfiedWith(klass)) {
                satisfyDependencyWith(contentValues, forger.mContext.get(klass));
                return;
              }
            }

            throw new UnsupportedOperationException("Forger cannot automatically satisfy dependency for polymorphic relationship. Please provide object with Forger.relatedTo(Object o) or add the parent object to context using Forger.inContextOf().");
          }
        });
      }
    });
  }

  public <T> ModelBuilder<T> iNeed(Class<T> klass) {
    return new ModelBuilder<T>(klass);
  }

  public Forger<TModel> inContextOf(Object o) {
    Preconditions.checkArgument(mModels.containsKey(o.getClass()), "Cannot create faking context for " + o.getClass().getName() + ", because it's not a part of ModelGraph.");

    HashMap<Class<?>, Object> contextCopy = Maps.newLinkedHashMap(mContext);
    contextCopy.put(o.getClass(), o);

    return new Forger<TModel>(this, contextCopy);
  }

  public <TContext> ContextBuilder<TContext> inContextOf(Class<TContext> klass) {
    Preconditions.checkArgument(mModels.containsKey(klass), "Cannot create faking context for " + klass.getName() + ", because it's not a part of ModelGraph.");

    return new ContextBuilder<TContext>(klass);
  }

  public class ContextBuilder<TContext> {
    private final ModelBuilder<TContext> mBuilder;

    private ContextBuilder(Class<TContext> klass) {
      mBuilder = iNeed(klass);
    }

    public ContextBuilder<TContext> with(String key, Object value) {
      mBuilder.with(key, value);
      return this;
    }

    public Forger<TModel> in(ContentResolver resolver) {
      return inContextOf(mBuilder.in(resolver));
    }
  }

  public class ModelBuilder<T> {
    private final TModel mModel;
    private final Class<T> mKlass;
    private ContentValues mContentValues;
    private Set<String> mPrimitiveColumns = Sets.newHashSet();
    private Set<String> mReadonlyColumns = Sets.newHashSet();

    private ModelBuilder(Class<T> klass) {
      mKlass = klass;

      mModel = mModels.get(klass);
      Preconditions.checkNotNull(mModel, "Forger cannot create an object of " + klass.getSimpleName() + " from the provided ModelGraph");

      mContentValues = initializeContentValues();
    }

    public ModelBuilder<T> relatedTo(Object... parentObjects) {
      Preconditions.checkNotNull(parentObjects);

      for (Object parentObject : parentObjects) {
        satisfyDependencyWith(parentObject);
      }

      return this;
    }

    private void satisfyDependencyWith(final Object parentObject) {
      Preconditions.checkNotNull(parentObject);

      Collection<Dependency<TModel>> dependencies = Collections2.filter(mDependencies.get(mKlass), new Predicate<Dependency<TModel>>() {
        @Override
        public boolean apply(Dependency<TModel> dependency) {
          return dependency.canBeSatisfiedWith(parentObject.getClass());
        }
      });

      switch (dependencies.size()) {
      case 0:
        throw new IllegalArgumentException(mKlass.getName() + " model is not related to " + parentObject.getClass().getName());
      case 1:
        Iterables.get(dependencies, 0).satisfyDependencyWith(mContentValues, parentObject);
        break;
      default:
        throw new IllegalStateException();
      }
    }

    public ModelBuilder<T> with(String key, Object value) {
      Preconditions.checkArgument(value != null || !mPrimitiveColumns.contains(key), "Cannot override column for primitive field with null");
      Preconditions.checkArgument(!mReadonlyColumns.contains(key), "Cannot override readonly column");

      putIntoContentValues(mContentValues, key, value);
      return this;
    }

    public T in(ContentResolver resolver) {
      for (Dependency<TModel> dependency : mDependencies.get(mKlass)) {
        Collection<String> keysOf = getKeysOf(mContentValues);
        Collection columns = dependency.getColumns();
        if (Collections.disjoint(keysOf, columns)) {
          dependency.satisfyDependencyWithNewObject(mContentValues, Forger.this, resolver);
        } else if (!keysOf.containsAll(columns)) {
          throw new IllegalStateException("Either override columns [" + Joiner.on(", ").join(columns) + "] using Forger.with(), or satisfy this dependency of " + mKlass.getSimpleName() + " using Forger.relatedTo().");
        }
      }

      Uri uri = resolver.insert(mModel.getUri(), mContentValues);

      Cursor c = resolver.query(uri, mMicroOrm.getProjection(mKlass), null, null, null);
      if (c != null && c.moveToFirst()) {
        return mMicroOrm.fromCursor(c, mKlass);
      } else {
        throw new IllegalStateException("ContentResolver returned null or empty Cursor.");
      }
    }

    private ContentValues initializeContentValues() {
      T fake = instantiateFake(mKlass);

      Collection<String> dependenciesColumns = Lists.newArrayList();
      for (Dependency<?> dependency : mDependencies.get(mKlass)) {
        dependenciesColumns.addAll(dependency.getColumns());
      }

      fillFake(mKlass, fake, dependenciesColumns);

      ContentValues values = mMicroOrm.toContentValues(fake);
      for (String column : dependenciesColumns) {
        values.remove(column);
      }

      return values;
    }

    private void fillFake(Class<?> klass, Object fake, Collection<String> dependenciesColumns) {
      try {
        for (Field field : Fields.allFieldsIncludingPrivateAndSuper(klass)) {
          boolean wasAccessible = field.isAccessible();
          field.setAccessible(true);

          if (field.getAnnotation(Column.class) != null) {
            Column columnAnnotation = field.getAnnotation(Column.class);
            if (field.getType().isPrimitive()) {
              mPrimitiveColumns.add(columnAnnotation.value());
            }

            if (columnAnnotation.readonly()) {
              mReadonlyColumns.add(columnAnnotation.value());
            }

            if (!dependenciesColumns.contains(columnAnnotation.value())) {
              if (!columnAnnotation.treatNullAsDefault()) {
                Class<?> fieldType = field.getType();

                Preconditions.checkArgument(mGenerators.containsKey(fieldType), "Forger doesn't know how to fake the " + fieldType.getName());
                field.set(fake, mGenerators.get(fieldType).generate());
              }
            }
          } else if (field.getAnnotation(Embedded.class) != null) {
            Object embeddedObject = instantiateFake(field.getType());
            fillFake(field.getType(), embeddedObject, dependenciesColumns);
            field.set(fake, embeddedObject);
          }

          field.setAccessible(wasAccessible);
        }
      } catch (IllegalAccessException e) {
        throw new IllegalArgumentException("Forger cannot initialize fields in " + klass.getSimpleName() + ".", e);
      }
    }

    private <T> T instantiateFake(Class<T> klass) {
      try {
        return klass.newInstance();
      } catch (Exception e) {
        throw new IllegalArgumentException("Forger cannot create the " + klass.getSimpleName() + ".", e);
      }
    }
  }

  private static Collection<String> getKeysOf(ContentValues values) {
    return Collections2.transform(values.valueSet(), new Function<Map.Entry<String, Object>, String>() {
      @Override
      public String apply(Map.Entry<String, Object> entry) {
        return entry.getKey();
      }
    });
  }

  private void putIntoContentValues(ContentValues values, String key, Object o) {
    if (o == null) {
      values.putNull(key);
    } else if (o instanceof Boolean) {
      values.put(key, (Boolean) o);
    } else if (o instanceof Number) {
      values.put(key, ((Number) o).longValue());
    } else {
      values.put(key, o.toString());
    }
  }

  private static Map<Class<?>, FakeDataGenerator<?>> getDefaultGenerators() {
    return ImmutableMap.<Class<?>, FakeDataGenerator<?>>builder()
        .put(String.class, new FakeDataGenerators.StringGenerator())
        .put(short.class, new FakeDataGenerators.ShortGenerator())
        .put(int.class, new FakeDataGenerators.IntegerGenerator())
        .put(long.class, new FakeDataGenerators.LongGenerator())
        .put(boolean.class, new FakeDataGenerators.BooleanGenerator())
        .put(float.class, new FakeDataGenerators.FloatGenerator())
        .put(double.class, new FakeDataGenerators.DoubleGenerator())
        .put(Short.class, new FakeDataGenerators.ShortGenerator())
        .put(Integer.class, new FakeDataGenerators.IntegerGenerator())
        .put(Long.class, new FakeDataGenerators.LongGenerator())
        .put(Boolean.class, new FakeDataGenerators.BooleanGenerator())
        .put(Float.class, new FakeDataGenerators.FloatGenerator())
        .put(Double.class, new FakeDataGenerators.DoubleGenerator())
        .build();
  }
}
