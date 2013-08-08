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
import com.chalup.thneed.PolymorphicType;
import com.google.common.collect.ImmutableList;

import android.content.ContentResolver;
import android.net.Uri;

import java.util.UUID;

public final class TestModels {
  private TestModels() {
  }

  public static class Identifiable {
    @Column("id")
    public long id;
  }

  public static class Deal extends Identifiable {
    @Column("contact_id")
    public long contactId;
    @Column("user_id")
    public long userId;
  }

  public static class User extends Identifiable {
    @Column("email")
    public String email;
  }

  public static class Contact extends Identifiable {
    @Column("contact_id")
    public Long contactId;

    @Column("user_id")
    public long userId;
  }

  public static class Lead extends Identifiable {
  }

  public static class ContactData extends Identifiable {
    @Column("lead_id")
    public long leadId;
  }

  public static class DealContact extends Identifiable {
    @Column("contact_id")
    public long contactId;

    @Column("deal_id")
    public long dealId;
  }

  public static class Note extends Identifiable {
    @Column("notable_type")
    public String notableType;

    @Column("notable_id")
    public long notableId;
  }

  public static class Call extends Identifiable {
    @Column("callable_type")
    public String callableType;

    @Column("callable_id")
    public long callableId;
  }

  public static class Tag extends Identifiable {
    @Column("value")
    public String value;
  }

  public static class Tagging extends Identifiable {
    @Column("taggable_type")
    public String taggableType;

    @Column("taggable_id")
    public long taggableId;

    @Column("tag_id")
    public long tagId;

    @Column("user_id")
    public long userId;
  }

  public static class ClassWithoutDefaultConstructor extends Identifiable {
    public ClassWithoutDefaultConstructor(Object unused) {
    }
  }

  public static class ClassWithoutPublicDefaultConstructor extends Identifiable {
    private ClassWithoutPublicDefaultConstructor() {
    }
  }

  public static class ClassWithNonBasicFieldType extends Identifiable {
    @Column("uuid")
    public UUID uuid;
  }

  public interface TestModel extends ContentResolverModel, MicroOrmModel {
  }

  public static class BaseTestModel implements TestModel {
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

  public static class PolyModel extends BaseTestModel implements PolymorphicType<TestModel, PolyModel> {
    private final String mModelName;

    public PolyModel(Class<?> klass, String modelName) {
      super(klass);
      mModelName = modelName;
    }

    @Override
    public PolyModel getModel() {
      return this;
    }

    @Override
    public String getModelName() {
      return mModelName;
    }
  }

  public static PolyModel CONTACT = new PolyModel(Contact.class, "Contact");
  public static PolyModel DEAL = new PolyModel(Deal.class, "Deal");
  public static TestModel USER = new BaseTestModel(User.class);
  public static PolyModel LEAD = new PolyModel(Lead.class, "Lead");
  public static TestModel CONTACT_DATA = new BaseTestModel(ContactData.class);
  public static TestModel DEAL_CONTACT = new BaseTestModel(DealContact.class);
  public static TestModel NOTE = new BaseTestModel(Note.class);
  public static TestModel CALL = new BaseTestModel(Call.class);
  public static TestModel TAG = new BaseTestModel(Tag.class);
  public static TestModel TAGGING = new BaseTestModel(Tagging.class);

  static ModelGraph<TestModel> MODEL_GRAPH = ModelGraph.of(TestModel.class)
      .with(new BaseTestModel(ClassWithoutDefaultConstructor.class))
      .with(new BaseTestModel(ClassWithoutPublicDefaultConstructor.class))
      .with(new BaseTestModel(ClassWithNonBasicFieldType.class))
      .with(USER)
      .where()
      .the(DEAL).references(CONTACT).by("contact_id")
      .the(CONTACT_DATA).isPartOf(LEAD).identified().by("lead_id")
      .the(DEAL_CONTACT).links(DEAL).by("deal_id").with(CONTACT).by("contact_id")
      .the(NOTE).references(ImmutableList.of(CONTACT, DEAL, LEAD)).by("notable_type", "notable_id")
      .the(CALL).references(ImmutableList.of(CONTACT, LEAD)).by("callable_type", "callable_id")
      .the(CONTACT).groupsOther().by("contact_id")
      .the(TAGGING).links(TAG).by("tag_id").with(ImmutableList.of(CONTACT, LEAD, DEAL)).by("taggable_type", "taggable_id")
      .the(TAGGING).references(USER).by("user_id")
      .the(DEAL).references(USER).by("user_id")
      .the(CONTACT).references(USER).by("user_id")
      .build();

  private static Uri buildUriFor(Class<?> klass) {
    return new Uri.Builder()
        .scheme(ContentResolver.SCHEME_CONTENT)
        .authority(TestModels.class.getPackage().getName())
        .appendPath(klass.getSimpleName().toLowerCase())
        .build();
  }
}
