package com.getbase.forger.tests;

import static org.fest.assertions.api.Assertions.assertThat;

import com.getbase.forger.Forger;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import org.chalup.microorm.MicroOrm;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.content.ContentResolver;

import java.util.List;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class GroupFakingTest {

  Forger<TestModels.TestModel> mTestSubject;
  ContentResolver mContentResolver;

  @Before
  public void setUp() throws Exception {
    mTestSubject = new Forger<>(TestModels.MODEL_GRAPH, new MicroOrm());
    mContentResolver = EchoContentResolver.get();
  }

  @Test
  public void shouldCreatePlentyOfObjects() throws Exception {
    List<TestModels.User> users = mTestSubject
        .iNeed(20)
        .of(TestModels.User.class)
        .in(mContentResolver);
    Set<Long> ids =
        FluentIterable.from(users)
            .transform(new Function<TestModels.User, Long>() {
              @Override
              public Long apply(TestModels.User user) {
                return user._id;
              }
            })
            .toSet();

    assertThat(ids).hasSize(20);
  }

  @Test
  public void shouldCreatePlentyOfObjectsWithGivenValue() throws Exception {
    List<TestModels.User> users = mTestSubject
        .iNeed(5)
        .of(TestModels.User.class)
        .with("email", "barrack@usa.gov")
        .in(mContentResolver);
    assertThat(users).hasSize(5);
    assertThat(
        Iterables.all(users, new Predicate<TestModels.User>() {
          @Override
          public boolean apply(TestModels.User user) {
            return "barrack@usa.gov".equals(user.email);
          }
        }))
        .isTrue();
  }

  @Test
  public void shouldCreatePlentyOfStuffInContextOfSomething() throws Exception {
    final TestModels.Contact contact = mTestSubject.iNeed(TestModels.Contact.class).in(mContentResolver);
    assertThat(contact).isNotNull();

    final Forger<TestModels.TestModel> contextualForger = mTestSubject.inContextOf(contact);

    List<TestModels.Deal> deals = contextualForger.iNeed(10).of(TestModels.Deal.class).in(mContentResolver);
    assertThat(deals).hasSize(10);
    assertThat(
        Iterables.all(deals, new Predicate<TestModels.Deal>() {
          @Override
          public boolean apply(TestModels.Deal deal) {
            return deal.contactId == contact.id;
          }
        }))
        .isTrue();
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldCrashIfPassedAmountIsLowerThanZero() throws Exception {
    mTestSubject.iNeed(-5);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldCrashIfPassedAmountIsZero() throws Exception {
    mTestSubject.iNeed(0);
  }

  @Test
  public void insertedChildObjectsShouldHaveDifferentParents() throws Exception {
    List<TestModels.Deal> deals = mTestSubject.iNeed(10).of(TestModels.Deal.class).in(mContentResolver);
    final ImmutableSet<Long> setOfIds =
        FluentIterable.from(deals)
            .transform(new Function<TestModels.Deal, Long>() {
              @Override
              public Long apply(TestModels.Deal deal) {
                return deal.contactId;
              }
            })
            .toSet();
    assertThat(setOfIds).hasSize(10);
  }
}
