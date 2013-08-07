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

package com.chalup.faker;

import com.chalup.faker.thneed.ContentResolverModel;
import com.chalup.faker.thneed.MicroOrmModel;
import com.chalup.microorm.MicroOrm;
import com.chalup.microorm.annotations.Column;
import com.chalup.thneed.ManyToManyRelationship;
import com.chalup.thneed.ModelGraph;
import com.chalup.thneed.ModelVisitor;
import com.chalup.thneed.OneToManyRelationship;
import com.chalup.thneed.OneToOneRelationship;
import com.chalup.thneed.PolymorphicRelationship;
import com.chalup.thneed.RecursiveModelRelationship;
import com.chalup.thneed.RelationshipVisitor;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

public class Faker<TModel extends ContentResolverModel & MicroOrmModel> {

  private final Map<Class<?>, TModel> mModels = Maps.newHashMap();
  private final MicroOrm mMicroOrm;
  private final Map<Class<?>, FakeDataGenerator<?>> mGenerators;
  private final Multimap<Class<?>, Dependency> mDependencies = HashMultimap.create();

  private interface Dependency {
    Class<?> getDependencyClass();

    Collection<String> getColumns();
  }

  public Faker(ModelGraph<TModel> modelGraph, MicroOrm microOrm) {
    this(modelGraph, microOrm, getDefaultGenerators());
  }

  private Faker(ModelGraph<TModel> modelGraph, MicroOrm microOrm, Map<Class<?>, FakeDataGenerator<?>> generators) {
    mMicroOrm = microOrm;
    mGenerators = generators;

    modelGraph.accept(new ModelVisitor<TModel>() {
      @Override
      public void visit(TModel model) {
        mModels.put(model.getModelClass(), model);
      }
    });

    modelGraph.accept(new RelationshipVisitor<TModel>() {
      @Override
      public void visit(final OneToManyRelationship<? extends TModel> relationship) {
        TModel model = relationship.mModel;
        mDependencies.put(model.getModelClass(), new Dependency() {
          @Override
          public Class<?> getDependencyClass() {
            TModel referencedModel = relationship.mReferencedModel;
            return referencedModel.getModelClass();
          }

          @Override
          public Collection<String> getColumns() {
            return Lists.newArrayList(relationship.mLinkedByColumn);
          }
        });
      }

      @Override
      public void visit(OneToOneRelationship<? extends TModel> oneToOneRelationship) {
        throw new UnsupportedOperationException("not implemented");
      }

      @Override
      public void visit(RecursiveModelRelationship<? extends TModel> recursiveModelRelationship) {
        throw new UnsupportedOperationException("not implemented");
      }

      @Override
      public void visit(ManyToManyRelationship<? extends TModel> manyToManyRelationship) {
        throw new UnsupportedOperationException("not implemented");
      }

      @Override
      public void visit(PolymorphicRelationship<? extends TModel> polymorphicRelationship) {
        throw new UnsupportedOperationException("not implemented");
      }
    });
  }

  public <T> ModelBuilder<T> iNeed(Class<T> klass) {
    return new ModelBuilder<T>(klass);
  }

  public class ModelBuilder<T> {
    private final TModel mModel;
    private final Class<T> mKlass;

    private ModelBuilder(Class<T> klass) {
      mKlass = klass;

      mModel = mModels.get(klass);
      Preconditions.checkNotNull(mModel, "Faker cannot create an object of " + klass.getSimpleName() + " from the provided ModelGraph");
    }

    public ModelBuilder<T> relatedTo(Object parentObject) {
      // TODO: check if the supplied object creates a relation

      return this;
    }

    public T in(ContentResolver resolver) {
      Uri uri = resolver.insert(mModel.getUri(), getContentValues());

      Cursor c = resolver.query(uri, mMicroOrm.getProjection(mKlass), null, null, null);
      if (c != null && c.moveToFirst()) {
        return mMicroOrm.fromCursor(c, mKlass);
      } else {
        throw new IllegalStateException("ContentResolver returned null or empty Cursor.");
      }
    }

    private ContentValues getContentValues() {
      T fake = instantiateFake();

      Collection<String> dependenciesColumns = Lists.newArrayList();
      for (Dependency dependency : mDependencies.get(mKlass)) {
        dependenciesColumns.addAll(dependency.getColumns());
      }

      try {
        for (Field field : Fields.allFieldsIncludingPrivateAndSuper(mKlass)) {
          boolean wasAccessible = field.isAccessible();
          field.setAccessible(true);

          Column columnAnnotation = field.getAnnotation(Column.class);
          if (columnAnnotation != null) {
            if (!dependenciesColumns.contains(columnAnnotation.value())) {
              Class<?> fieldType = field.getType();

              Preconditions.checkArgument(mGenerators.containsKey(fieldType), "Faker doesn't know how to fake the " + fieldType.getName());
              field.set(fake, mGenerators.get(fieldType).generate());
            }
          }

          field.setAccessible(wasAccessible);
        }
      } catch (IllegalAccessException e) {
        throw new IllegalArgumentException("Faker cannot initialize fields in " + mKlass.getSimpleName() + ".", e);
      }

      return mMicroOrm.toContentValues(fake);
    }

    private T instantiateFake() {
      try {
        return mKlass.newInstance();
      } catch (Exception e) {
        throw new IllegalArgumentException("Faker cannot create the " + mKlass.getSimpleName() + ".", e);
      }
    }
  }

  private static final Map<Class<?>, FakeDataGenerator<?>> getDefaultGenerators() {
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
