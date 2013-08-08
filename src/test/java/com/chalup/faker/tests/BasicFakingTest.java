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

import static org.fest.assertions.Assertions.assertThat;

import com.chalup.faker.Faker;
import com.chalup.microorm.MicroOrm;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.content.ContentResolver;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class BasicFakingTest {

  Faker<TestModels.TestModel> mTestSubject;
  ContentResolver mContentResolver;

  @Before
  public void setUp() throws Exception {
    mTestSubject = new Faker<TestModels.TestModel>(TestModels.MODEL_GRAPH, new MicroOrm());
    mContentResolver = EchoContentResolver.get();
  }

  public static class ClassOutsideOfTheModelGraph {
  }

  @Test(expected = NullPointerException.class)
  public void shouldNotAllowFakingClassesOutsideOfModelGraph() throws Exception {
    mTestSubject.iNeed(ClassOutsideOfTheModelGraph.class).in(mContentResolver);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldNotAllowCreatingClassWithoutDefaultConstructor() throws Exception {
    mTestSubject.iNeed(TestModels.ClassWithoutDefaultConstructor.class).in(mContentResolver);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldNotAllowCreatingClassWithoutPublicDefaultConstructor() throws Exception {
    mTestSubject.iNeed(TestModels.ClassWithoutPublicDefaultConstructor.class).in(mContentResolver);
  }

  @Test
  public void shouldCreateSimpleObject() throws Exception {
    TestModels.User user = mTestSubject.iNeed(TestModels.User.class).in(mContentResolver);

    assertThat(user).isNotNull();

    assertThat(user.id).isNotEqualTo(0);
    assertThat(user.email).isNotNull();
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

  @Test(expected = IllegalArgumentException.class)
  public void shouldRejectIncorrectParentObject() throws Exception {
    mTestSubject.iNeed(TestModels.Deal.class).relatedTo(new Object());
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldNotAllowCreatingClassWithNonBasicMemberTypes() throws Exception {
    mTestSubject.iNeed(TestModels.ClassWithNonBasicFieldType.class).in(mContentResolver);
  }

  @Test
  public void shouldRecursivelySatisfyDependenciesWithNewObjectsInOneToOneRelationship() throws Exception {
    TestModels.ContactData contactData = mTestSubject.iNeed(TestModels.ContactData.class).in(mContentResolver);

    assertThat(contactData).isNotNull();
    assertThat(contactData.leadId).isNotEqualTo(0);
  }

  @Test
  public void shouldAllowSupplyingParentObjectForOneToOneRelationship() throws Exception {
    TestModels.Lead lead = mTestSubject.iNeed(TestModels.Lead.class).in(mContentResolver);
    assertThat(lead).isNotNull();

    TestModels.ContactData contactData = mTestSubject.iNeed(TestModels.ContactData.class).relatedTo(lead).in(mContentResolver);
    assertThat(contactData).isNotNull();
    assertThat(contactData.leadId).isEqualTo(lead.id);
  }

  @Test
  public void shouldRecursivelySatisfyDependenciesWithNewObjectsInManyToManyRelationship() throws Exception {
    TestModels.DealContact dealContact = mTestSubject.iNeed(TestModels.DealContact.class).in(mContentResolver);

    assertThat(dealContact).isNotNull();
    assertThat(dealContact.contactId).isNotEqualTo(0);
    assertThat(dealContact.dealId).isNotEqualTo(0);
  }

  @Test
  public void shouldAllowSupplyingObjectsForManyToManyRelationship() throws Exception {
    TestModels.Contact contact = mTestSubject.iNeed(TestModels.Contact.class).in(mContentResolver);
    assertThat(contact).isNotNull();

    TestModels.Deal deal = mTestSubject.iNeed(TestModels.Deal.class).in(mContentResolver);
    assertThat(deal).isNotNull();

    TestModels.DealContact dealContact = mTestSubject
        .iNeed(TestModels.DealContact.class)
        .relatedTo(contact)
        .relatedTo(deal)
        .in(mContentResolver);
    assertThat(dealContact).isNotNull();
    assertThat(dealContact.contactId).isEqualTo(contact.id);
    assertThat(dealContact.dealId).isEqualTo(deal.id);
  }

  @Test
  public void shouldAllowSupplyingObjectsForOnlyOneSideOfManyToManyRelationship() throws Exception {
    TestModels.Contact contact = mTestSubject.iNeed(TestModels.Contact.class).in(mContentResolver);
    assertThat(contact).isNotNull();

    TestModels.DealContact dealContact = mTestSubject
        .iNeed(TestModels.DealContact.class)
        .relatedTo(contact)
        .in(mContentResolver);
    assertThat(dealContact).isNotNull();
    assertThat(dealContact.contactId).isEqualTo(contact.id);
    assertThat(dealContact.dealId).isNotEqualTo(0);
  }
}
