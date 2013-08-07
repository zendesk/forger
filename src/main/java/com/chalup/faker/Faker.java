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
import com.chalup.thneed.ModelGraph;
import com.chalup.thneed.ModelVisitor;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import java.util.Map;

public class Faker<TModel extends ContentResolverModel & MicroOrmModel> {

  private final Map<Class<?>, TModel> mModels = Maps.newHashMap();
  private final MicroOrm mMicroOrm;

  public Faker(ModelGraph<TModel> modelGraph, MicroOrm microOrm) {
    mMicroOrm = microOrm;
    modelGraph.accept(new ModelVisitor<TModel>() {
      @Override
      public void visit(TModel model) {
        mModels.put(model.getModelClass(), model);
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

      // TODO: initialize @Column annotated fields with fake values

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
}
