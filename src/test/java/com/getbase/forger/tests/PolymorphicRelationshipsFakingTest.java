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

package com.getbase.forger.tests;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import com.getbase.forger.Forger;

import org.chalup.microorm.MicroOrm;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.content.ContentResolver;
import android.content.ContentValues;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class PolymorphicRelationshipsFakingTest {

  Forger<TestModels.TestModel> mTestSubject;
  ContentResolver mContentResolver;

  @Before
  public void setUp() throws Exception {
    mTestSubject = new Forger<TestModels.TestModel>(TestModels.MODEL_GRAPH, new MicroOrm());
    mContentResolver = EchoContentResolver.get();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void shouldNotAllowFakingOfObjectWithMandatoryPolymorphicRelationshipWithoutSuppliedObject() throws Exception {
    mTestSubject.iNeed(TestModels.Note.class).in(mContentResolver);
  }

  @Test
  public void shouldAllowSupplyingObjectsForPolymorphicRelationship() throws Exception {
    TestModels.Contact contact = mTestSubject.iNeed(TestModels.Contact.class).in(mContentResolver);
    assertThat(contact).isNotNull();

    TestModels.Note note = mTestSubject
        .iNeed(TestModels.Note.class)
        .relatedTo(contact)
        .in(mContentResolver);

    assertThat(note).isNotNull();
    assertThat(note.notableId).isEqualTo(contact.id);
    assertThat(note.notableType).isEqualTo("Contact");
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldValidateSuppliedPolymorphicObjectType() throws Exception {
    TestModels.Deal deal = mTestSubject.iNeed(TestModels.Deal.class).in(mContentResolver);
    assertThat(deal).isNotNull();

    mTestSubject.iNeed(TestModels.Call.class).relatedTo(deal);
  }

  @Test
  public void shouldNotTryToSatisfyDependenciesForOverriddenFieldsOfPolymorphicRelationship() throws Exception {
    String notableType = "Contact";
    long notableId = 42L;

    TestModels.Note note = mTestSubject.iNeed(TestModels.Note.class)
        .with("notable_type", notableType)
        .with("notable_id", notableId)
        .in(mContentResolver);

    assertThat(note).isNotNull();
    assertThat(note.notableType).isEqualTo(notableType);
    assertThat(note.notableId).isEqualTo(notableId);

    verify(mContentResolver, never()).insert(Matchers.eq(TestModels.CONTACT.getUri()), any(ContentValues.class));
    verify(mContentResolver, never()).insert(Matchers.eq(TestModels.DEAL.getUri()), any(ContentValues.class));
    verify(mContentResolver, never()).insert(Matchers.eq(TestModels.LEAD.getUri()), any(ContentValues.class));
  }

  @Test(expected = IllegalStateException.class)
  public void shouldNotAllowOverridingOnlyOneColumnInPolymorphicRelationship() throws Exception {
    mTestSubject.iNeed(TestModels.Note.class)
        .with("notable_id", 42L)
        .in(mContentResolver);
  }

  @Test
  public void shouldSatisfyPolymorphicDependenciesFromSuppliedContext() throws Exception {
    TestModels.Contact contact = mTestSubject.iNeed(TestModels.Contact.class).in(mContentResolver);
    assertThat(contact).isNotNull();

    Forger<TestModels.TestModel> forgerWithContext = mTestSubject.inContextOf(contact);

    TestModels.Note note = forgerWithContext.iNeed(TestModels.Note.class).in(mContentResolver);
    assertThat(note).isNotNull();
    assertThat(note.notableId).isEqualTo(contact.id);
    assertThat(note.notableType).isEqualTo("Contact");
  }

  @Test
  public void shouldUseTheLatestContextObjectToSatisfyPolymorphicDependenciesFromSuppliedContext() throws Exception {
    Forger<TestModels.TestModel> forgerWithContext = mTestSubject
        .inContextOf(TestModels.Contact.class).in(mContentResolver)
        .inContextOf(TestModels.Deal.class).in(mContentResolver);

    TestModels.Note note = forgerWithContext.iNeed(TestModels.Note.class).in(mContentResolver);
    assertThat(note).isNotNull();

    assertThat(note.notableType).isEqualTo("Deal");
  }
}
