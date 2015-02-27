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

import static org.fest.assertions.api.Assertions.assertThat;
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
public class OneToManyRelationshipsFakingTest {

  Forger<TestModels.TestModel> mTestSubject;
  ContentResolver mContentResolver;

  @Before
  public void setUp() throws Exception {
    mTestSubject = new Forger<>(TestModels.MODEL_GRAPH, new MicroOrm());
    mContentResolver = EchoContentResolver.get();
  }

  @Test
  public void shouldRecursivelySatisfyDependenciesWithNewObjectsInOneToManyRelationship() throws Exception {
    TestModels.Deal deal = mTestSubject.iNeed(TestModels.Deal.class).in(mContentResolver);

    assertThat(deal).isNotNull();
    assertThat(deal.contactId).isNotEqualTo(0);
  }

  @Test
  public void shouldAllowSupplyingParentObjectForOneToManyRelationship() throws Exception {
    TestModels.Contact contact = mTestSubject.iNeed(TestModels.Contact.class).in(mContentResolver);
    assertThat(contact).isNotNull();

    TestModels.Deal deal = mTestSubject.iNeed(TestModels.Deal.class).relatedTo(contact).in(mContentResolver);
    assertThat(deal).isNotNull();

    assertThat(deal.contactId).isEqualTo(contact.id);
  }

  @Test
  public void shouldNotTryToSatisfyDependenciesForOverriddenFieldsOfOneToManyRelationship() throws Exception {
    long contactId = 42L;
    TestModels.Deal deal = mTestSubject.iNeed(TestModels.Deal.class).with("contact_id", contactId).in(mContentResolver);

    assertThat(deal).isNotNull();
    assertThat(deal.contactId).isEqualTo(contactId);

    verify(mContentResolver, never()).insert(Matchers.eq(TestModels.CONTACT.getUri()), any(ContentValues.class));
  }

  @Test
  public void shouldSatisfyOneToManyDependenciesFromSuppliedContext() throws Exception {
    TestModels.Contact contact = mTestSubject.iNeed(TestModels.Contact.class).in(mContentResolver);
    assertThat(contact).isNotNull();

    Forger<TestModels.TestModel> forgerWithContext = mTestSubject.inContextOf(contact);

    TestModels.Deal deal = forgerWithContext.iNeed(TestModels.Deal.class).in(mContentResolver);
    assertThat(deal).isNotNull();
    assertThat(deal.contactId).isEqualTo(contact.id);
  }
}
