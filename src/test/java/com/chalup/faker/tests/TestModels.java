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

package com.chalup.faker.tests;

import com.chalup.faker.thneed.ContentResolverModel;
import com.chalup.faker.thneed.MicroOrmModel;
import com.chalup.microorm.annotations.Column;
import com.chalup.thneed.ModelGraph;

import android.content.ContentResolver;
import android.net.Uri;

import java.util.UUID;

public final class TestModels {
  private TestModels() {
  }

  public static class Room {
    @Column("id")
    public long id;
    @Column("name")
    public String name;
  }

  public static class ClassWithoutDefaultConstructor {
    public ClassWithoutDefaultConstructor(Object unused) {
    }
  }

  public static class ClassWithoutPublicDefaultConstructor {
    private ClassWithoutPublicDefaultConstructor() {
    }
  }

  public static class ClassWithNonBasicFieldType {
    @Column("uuid")
    public UUID uuid;
  }

  public static abstract class TestModel implements ContentResolverModel, MicroOrmModel {
  }

  public static class BaseTestModel extends TestModel {
    private final Class<?> mKlass;

    public BaseTestModel(Class<?> klass) {
      mKlass = klass;
    }

    @Override
    public Uri getUri() {
      return buildUriFor(mKlass);
    }

    @Override
    public Class<?> getModelClass() {
      return mKlass;
    }
  }

  private static TestModel ROOM = new BaseTestModel(Room.class);

  static ModelGraph<TestModel> MODEL_GRAPH = ModelGraph.of(TestModel.class)
      .with(ROOM)
      .with(new BaseTestModel(ClassWithoutDefaultConstructor.class))
      .with(new BaseTestModel(ClassWithoutPublicDefaultConstructor.class))
      .with(new BaseTestModel(ClassWithNonBasicFieldType.class))
      .build();

  private static Uri buildUriFor(Class<?> klass) {
    return new Uri.Builder()
        .scheme(ContentResolver.SCHEME_CONTENT)
        .authority(TestModels.class.getPackage().getName())
        .appendPath(klass.getSimpleName().toLowerCase())
        .build();
  }
}
