/*
 * Copyright (C) 2016 Anthony C. Restaino
 * <p/>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.anthonycr.bonsai;

import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class ObservableUnitTest extends BaseUnitTest {

    @Test
    public void testMainLooperWorking() throws Exception {
        if (Looper.getMainLooper() == null) {
            Looper.prepareMainLooper();
        }
        assertNotNull(Looper.getMainLooper());
    }

    @Test
    public void testObservableEmissionOrder_singleThread() throws Exception {
        final int testCount = 7;

        final List<String> list = new ArrayList<>(testCount);
        Observable.create(new ObservableAction<String>() {
            @Override
            public void onSubscribe(@NonNull ObservableSubscriber<String> subscriber) {
                for (int n = 0; n < testCount; n++) {
                    subscriber.onNext(String.valueOf(n));
                }
                subscriber.onComplete();
            }
        }).subscribeOn(Schedulers.current())
            .observeOn(Schedulers.current())
            .subscribe(new ObservableOnSubscribe<String>() {
                @Override
                public void onNext(@Nullable String item) {
                    list.add(item);
                }

            });

        assertTrue(list.size() == testCount);
        for (int n = 0; n < list.size(); n++) {
            assertTrue(String.valueOf(n).equals(list.get(n)));
        }
    }

    @Test
    public void testObservableEventEmission_withException() throws Exception {
        final int testCount = 7;

        final Assertion<Boolean> errorAssertion = new Assertion<>(false);
        final Assertion<Boolean> nextAssertion = new Assertion<>(false);
        final Assertion<Boolean> completeAssertion = new Assertion<>(false);
        final Assertion<Boolean> startAssertion = new Assertion<>(false);

        final List<String> list = new ArrayList<>(testCount);
        Observable.create(new ObservableAction<String>() {
            @Override
            public void onSubscribe(@NonNull ObservableSubscriber<String> subscriber) {
                for (int n = 0; n < testCount; n++) {
                    subscriber.onNext(String.valueOf(n));
                }
                throw new RuntimeException("Test failure");
            }
        }).subscribeOn(Schedulers.current())
            .observeOn(Schedulers.current())
            .subscribe(new ObservableOnSubscribe<String>() {

                @Override
                public void onStart() {
                    startAssertion.set(true);
                }

                @Override
                public void onNext(@Nullable String item) {
                    nextAssertion.set(true);
                    list.add(item);
                }

                @Override
                public void onComplete() {
                    completeAssertion.set(true);
                }

                @Override
                public void onError(@NonNull Throwable throwable) {
                    errorAssertion.set(true);
                }
            });

        // Even though error has been broadcast,
        // observable should still complete.
        assertTrue(list.size() == testCount);
        for (int n = 0; n < list.size(); n++) {
            assertTrue(String.valueOf(n).equals(list.get(n)));
        }

        // Assert that each of the events was
        // received by the subscriber
        assertTrue(errorAssertion.get());
        assertTrue(startAssertion.get());
        assertFalse(completeAssertion.get());
        assertTrue(nextAssertion.get());
    }

    @Test
    public void testObservableEventEmission_withoutSubscriber_withException() throws Exception {
        final int testCount = 7;

        Observable.create(new ObservableAction<String>() {
            @Override
            public void onSubscribe(@NonNull ObservableSubscriber<String> subscriber) {
                for (int n = 0; n < testCount; n++) {
                    subscriber.onNext(String.valueOf(n));
                }
                throw new RuntimeException("Test failure");
            }
        }).subscribeOn(Schedulers.current())
            .observeOn(Schedulers.current())
            .subscribe();

        // No assertions since we did not supply an OnSubscribe.
    }

    @Test
    public void testObservableEventEmission_withError() throws Exception {
        final int testCount = 7;

        final Assertion<Boolean> errorAssertion = new Assertion<>(false);
        final Assertion<Boolean> nextAssertion = new Assertion<>(false);
        final Assertion<Boolean> completeAssertion = new Assertion<>(false);
        final Assertion<Boolean> startAssertion = new Assertion<>(false);

        final List<String> list = new ArrayList<>(testCount);
        Observable.create(new ObservableAction<String>() {
            @Override
            public void onSubscribe(@NonNull ObservableSubscriber<String> subscriber) {
                for (int n = 0; n < testCount; n++) {
                    subscriber.onNext(String.valueOf(n));
                }
                try {
                    throw new Exception("Test failure");
                } catch (Exception e) {
                    subscriber.onError(e);
                }
                subscriber.onComplete();
            }
        }).subscribeOn(Schedulers.current())
            .observeOn(Schedulers.current())
            .subscribe(new ObservableOnSubscribe<String>() {

                @Override
                public void onStart() {
                    startAssertion.set(true);
                }

                @Override
                public void onNext(@Nullable String item) {
                    nextAssertion.set(true);
                    list.add(item);
                }

                @Override
                public void onComplete() {
                    completeAssertion.set(true);
                }

                @Override
                public void onError(@NonNull Throwable throwable) {
                    errorAssertion.set(true);
                }
            });

        // Even though error has been broadcast,
        // observable should still complete.
        assertTrue(list.size() == testCount);
        for (int n = 0; n < list.size(); n++) {
            assertTrue(String.valueOf(n).equals(list.get(n)));
        }

        // Assert that each of the events was
        // received by the subscriber
        assertTrue(errorAssertion.get());
        assertTrue(startAssertion.get());
        assertFalse(completeAssertion.get());
        assertTrue(nextAssertion.get());
    }

    @Test
    public void testObservableEventEmission_withoutError() throws Exception {
        final int testCount = 7;

        final Assertion<Boolean> errorAssertion = new Assertion<>(false);
        final Assertion<Boolean> nextAssertion = new Assertion<>(false);
        final Assertion<Boolean> completeAssertion = new Assertion<>(false);
        final Assertion<Boolean> startAssertion = new Assertion<>(false);

        final List<String> list = new ArrayList<>(testCount);
        Observable.create(new ObservableAction<String>() {
            @Override
            public void onSubscribe(@NonNull ObservableSubscriber<String> subscriber) {
                for (int n = 0; n < testCount; n++) {
                    subscriber.onNext(String.valueOf(n));
                }
                subscriber.onComplete();
            }
        }).subscribeOn(Schedulers.current())
            .observeOn(Schedulers.current())
            .subscribe(new ObservableOnSubscribe<String>() {

                @Override
                public void onStart() {
                    startAssertion.set(true);
                }

                @Override
                public void onNext(@Nullable String item) {
                    nextAssertion.set(true);
                    list.add(item);
                }

                @Override
                public void onComplete() {
                    completeAssertion.set(true);
                }

                @Override
                public void onError(@NonNull Throwable throwable) {
                    errorAssertion.set(true);
                }
            });

        // Even though error has been broadcast,
        // observable should still complete.
        assertTrue(list.size() == testCount);
        for (int n = 0; n < list.size(); n++) {
            assertTrue(String.valueOf(n).equals(list.get(n)));
        }

        // Assert that each of the events was
        // received by the subscriber
        assertFalse(errorAssertion.get());
        assertTrue(startAssertion.get());
        assertTrue(completeAssertion.get());
        assertTrue(nextAssertion.get());
    }

    @Test
    public void testObservableUnsubscribe_unsubscribesSuccessfully() throws Exception {
        final CountDownLatch subscribeLatch = new CountDownLatch(1);
        final CountDownLatch latch = new CountDownLatch(1);
        final Assertion<Boolean> assertion = new Assertion<>(false);
        Subscription stringSubscription = Observable.create(new ObservableAction<String>() {

            @Override
            public void onSubscribe(@NonNull ObservableSubscriber<String> subscriber) {
                try {
                    subscribeLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                subscriber.onNext("test");
                latch.countDown();
            }
        }).subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribe(new ObservableOnSubscribe<String>() {
                @Override
                public void onNext(@Nullable String item) {
                    assertion.set(true);
                }
            });

        stringSubscription.unsubscribe();
        subscribeLatch.countDown();
        latch.await();

        assertFalse(assertion.get());
    }

    @Test
    public void testObservableThread_onStart_isCorrect() throws Exception {
        final CountDownLatch observeLatch = new CountDownLatch(1);
        final CountDownLatch subscribeLatch = new CountDownLatch(1);

        final Assertion<String> subscribeThreadAssertion = new Assertion<>();
        final Assertion<String> observerThreadAssertion = new Assertion<>();

        final Assertion<Boolean> onStartAssertion = new Assertion<>(false);
        final Assertion<Boolean> onErrorAssertion = new Assertion<>(false);

        Observable.create(new ObservableAction<String>() {

            @Override
            public void onSubscribe(@NonNull ObservableSubscriber<String> subscriber) {
                subscribeThreadAssertion.set(Thread.currentThread().toString());
                subscribeLatch.countDown();
                subscriber.onComplete();
            }
        }).subscribeOn(Schedulers.worker())
            .observeOn(Schedulers.io())
            .subscribe(new ObservableOnSubscribe<String>() {
                @Override
                public void onNext(@Nullable String item) {

                }

                @Override
                public void onError(@NonNull Throwable throwable) {
                    onErrorAssertion.set(true);
                    observerThreadAssertion.set(Thread.currentThread().toString());
                    observeLatch.countDown();
                }

                @Override
                public void onStart() {
                    onStartAssertion.set(true);
                    observerThreadAssertion.set(Thread.currentThread().toString());
                    observeLatch.countDown();
                }
            });

        subscribeLatch.await();
        observeLatch.await();

        String currentThread = Thread.currentThread().toString();

        assertNotNull(subscribeThreadAssertion.get());
        assertNotNull(observerThreadAssertion.get());

        assertNotEquals(subscribeThreadAssertion.get(), currentThread);
        assertNotEquals(observerThreadAssertion.get(), currentThread);
        assertNotEquals(subscribeThreadAssertion.get(), observerThreadAssertion.get());

        assertTrue(onStartAssertion.get());
        assertFalse(onErrorAssertion.get());
    }

    @Test
    public void testObservableThread_onNext_isCorrect() throws Exception {
        final CountDownLatch observeLatch = new CountDownLatch(1);
        final CountDownLatch subscribeLatch = new CountDownLatch(1);

        final Assertion<String> subscribeThreadAssertion = new Assertion<>();
        final Assertion<String> observerThreadAssertion = new Assertion<>();

        final Assertion<Boolean> onNextAssertion = new Assertion<>(false);
        final Assertion<Boolean> onErrorAssertion = new Assertion<>(false);

        Observable.create(new ObservableAction<String>() {

            @Override
            public void onSubscribe(@NonNull ObservableSubscriber<String> subscriber) {
                subscribeThreadAssertion.set(Thread.currentThread().toString());
                subscribeLatch.countDown();
                subscriber.onNext(null);
                subscriber.onComplete();
            }
        }).subscribeOn(Schedulers.worker())
            .observeOn(Schedulers.io())
            .subscribe(new ObservableOnSubscribe<String>() {
                @Override
                public void onError(@NonNull Throwable throwable) {
                    onErrorAssertion.set(true);
                    observerThreadAssertion.set(Thread.currentThread().toString());
                    observeLatch.countDown();
                }

                @Override
                public void onNext(@Nullable String item) {
                    onNextAssertion.set(true);
                    observerThreadAssertion.set(Thread.currentThread().toString());
                    observeLatch.countDown();
                }
            });

        subscribeLatch.await();
        observeLatch.await();

        String currentThread = Thread.currentThread().toString();

        assertNotNull(subscribeThreadAssertion.get());
        assertNotNull(observerThreadAssertion.get());

        assertNotEquals(subscribeThreadAssertion.get(), currentThread);
        assertNotEquals(observerThreadAssertion.get(), currentThread);
        assertNotEquals(subscribeThreadAssertion.get(), observerThreadAssertion.get());

        assertTrue(onNextAssertion.get());
        assertFalse(onErrorAssertion.get());
    }

    @Test
    public void testObservableThread_onComplete_isCorrect() throws Exception {
        final CountDownLatch observeLatch = new CountDownLatch(1);
        final CountDownLatch subscribeLatch = new CountDownLatch(1);

        final Assertion<String> subscribeThreadAssertion = new Assertion<>();
        final Assertion<String> observerThreadAssertion = new Assertion<>();

        final Assertion<Boolean> onCompleteAssertion = new Assertion<>(false);
        final Assertion<Boolean> onErrorAssertion = new Assertion<>(false);

        Observable.create(new ObservableAction<String>() {

            @Override
            public void onSubscribe(@NonNull ObservableSubscriber<String> subscriber) {
                subscribeThreadAssertion.set(Thread.currentThread().toString());
                subscribeLatch.countDown();
                subscriber.onComplete();
            }
        }).subscribeOn(Schedulers.worker())
            .observeOn(Schedulers.io())
            .subscribe(new ObservableOnSubscribe<String>() {
                @Override
                public void onError(@NonNull Throwable throwable) {
                    onErrorAssertion.set(true);
                    observerThreadAssertion.set(Thread.currentThread().toString());
                    observeLatch.countDown();
                }

                @Override
                public void onComplete() {
                    onCompleteAssertion.set(true);
                    observerThreadAssertion.set(Thread.currentThread().toString());
                    observeLatch.countDown();
                }
            });

        subscribeLatch.await();
        observeLatch.await();

        String currentThread = Thread.currentThread().toString();

        assertNotNull(subscribeThreadAssertion.get());
        assertNotNull(observerThreadAssertion.get());

        assertNotEquals(subscribeThreadAssertion.get(), currentThread);
        assertNotEquals(observerThreadAssertion.get(), currentThread);
        assertNotEquals(subscribeThreadAssertion.get(), observerThreadAssertion.get());

        assertTrue(onCompleteAssertion.get());
        assertFalse(onErrorAssertion.get());
    }

    @Test
    public void testObservableThread_onError_isCorrect() throws Exception {
        final CountDownLatch observeLatch = new CountDownLatch(1);
        final CountDownLatch subscribeLatch = new CountDownLatch(1);

        final Assertion<String> subscribeThreadAssertion = new Assertion<>();
        final Assertion<String> observerThreadAssertion = new Assertion<>();

        final Assertion<Boolean> onCompleteAssertion = new Assertion<>(false);
        final Assertion<Boolean> onErrorAssertion = new Assertion<>(false);

        Observable.create(new ObservableAction<String>() {

            @Override
            public void onSubscribe(@NonNull ObservableSubscriber<String> subscriber) {
                subscribeThreadAssertion.set(Thread.currentThread().toString());
                subscribeLatch.countDown();
                subscriber.onError(new RuntimeException("There was a problem"));
            }
        }).subscribeOn(Schedulers.worker())
            .observeOn(Schedulers.io())
            .subscribe(new ObservableOnSubscribe<String>() {
                @Override
                public void onError(@NonNull Throwable throwable) {
                    onErrorAssertion.set(true);
                    observerThreadAssertion.set(Thread.currentThread().toString());
                    observeLatch.countDown();
                }

                @Override
                public void onComplete() {
                    onCompleteAssertion.set(true);
                    observerThreadAssertion.set(Thread.currentThread().toString());
                    observeLatch.countDown();
                }
            });

        subscribeLatch.await();
        observeLatch.await();

        String currentThread = Thread.currentThread().toString();

        assertNotNull(subscribeThreadAssertion.get());
        assertNotNull(observerThreadAssertion.get());

        assertNotEquals(subscribeThreadAssertion.get(), currentThread);
        assertNotEquals(observerThreadAssertion.get(), currentThread);
        assertNotEquals(subscribeThreadAssertion.get(), observerThreadAssertion.get());

        assertFalse(onCompleteAssertion.get());
        assertTrue(onErrorAssertion.get());
    }

    @Test
    public void testObservableThread_ThrownException_isCorrect() throws Exception {
        final CountDownLatch observeLatch = new CountDownLatch(1);
        final CountDownLatch subscribeLatch = new CountDownLatch(1);

        final Assertion<String> subscribeThreadAssertion = new Assertion<>();
        final Assertion<String> observerThreadAssertion = new Assertion<>();

        final Assertion<Boolean> onCompleteAssertion = new Assertion<>(false);
        final Assertion<Boolean> onErrorAssertion = new Assertion<>(false);

        Observable.create(new ObservableAction<String>() {

            @Override
            public void onSubscribe(@NonNull ObservableSubscriber<String> subscriber) {
                subscribeThreadAssertion.set(Thread.currentThread().toString());
                subscribeLatch.countDown();
                throw new RuntimeException("There was a problem");
            }
        }).subscribeOn(Schedulers.worker())
            .observeOn(Schedulers.io())
            .subscribe(new ObservableOnSubscribe<String>() {
                @Override
                public void onError(@NonNull Throwable throwable) {
                    onErrorAssertion.set(true);
                    observerThreadAssertion.set(Thread.currentThread().toString());
                    observeLatch.countDown();
                }

                @Override
                public void onComplete() {
                    onCompleteAssertion.set(true);
                    observerThreadAssertion.set(Thread.currentThread().toString());
                    observeLatch.countDown();
                }
            });

        subscribeLatch.await();
        observeLatch.await();

        String currentThread = Thread.currentThread().toString();

        assertNotNull(subscribeThreadAssertion.get());
        assertNotNull(observerThreadAssertion.get());

        assertNotEquals(subscribeThreadAssertion.get(), currentThread);
        assertNotEquals(observerThreadAssertion.get(), currentThread);
        assertNotEquals(subscribeThreadAssertion.get(), observerThreadAssertion.get());

        assertFalse(onCompleteAssertion.get());
        assertTrue(onErrorAssertion.get());
    }

    @Test
    public void testObservableSubscribesWithoutSubscriber() throws Exception {
        final Assertion<Boolean> isCalledAssertion = new Assertion<>(false);
        Observable.create(new ObservableAction<Object>() {
            @Override
            public void onSubscribe(@NonNull ObservableSubscriber<Object> subscriber) {
                subscriber.onNext(null);
                subscriber.onComplete();
                isCalledAssertion.set(true);
            }
        }).subscribeOn(Schedulers.current())
            .observeOn(Schedulers.current())
            .subscribe();
        assertTrue("onSubscribe must be called when subscribe is called", isCalledAssertion.get());
    }

    @Test
    public void testObservableThrowsException_onCompleteCalledTwice() throws Exception {
        final Assertion<Boolean> errorThrown = new Assertion<>(false);
        Observable.create(new ObservableAction<Object>() {
            @Override
            public void onSubscribe(@NonNull ObservableSubscriber<Object> subscriber) {
                try {
                    subscriber.onComplete();
                    subscriber.onComplete();
                } catch (RuntimeException e) {
                    errorThrown.set(true);
                }
            }
        }).subscribe(new ObservableOnSubscribe<Object>() {
        });
        assertTrue("Exception should be thrown in subscribe code if onComplete called more than once",
            errorThrown.get());
    }

    @Test
    public void testObservableThrowsException_onNextCalledAfterOnComplete() throws Exception {
        final Assertion<Boolean> errorThrown = new Assertion<>(false);
        Observable.create(new ObservableAction<Object>() {
            @Override
            public void onSubscribe(@NonNull ObservableSubscriber<Object> subscriber) {
                try {
                    subscriber.onComplete();
                    subscriber.onNext(null);
                } catch (RuntimeException e) {
                    errorThrown.set(true);
                }
            }
        }).subscribe(new ObservableOnSubscribe<Object>() {
        });
        assertTrue("Exception should be thrown in subscribe code if onNext called after onComplete",
            errorThrown.get());
    }

    @Test
    public void testObservableCreatesLooperIfNotThere() throws Exception {
        final Assertion<Boolean> looperInitiallyNull = new Assertion<>(false);
        final Assertion<Boolean> looperFinallyNotNull = new Assertion<>(false);
        final CountDownLatch latch = new CountDownLatch(1);
        Schedulers.newSingleThreadedScheduler().execute(new Runnable() {
            @Override
            public void run() {
                // No looper associated with this thread yet
                looperInitiallyNull.set(Looper.myLooper() == null);
                Observable.create(new ObservableAction<Object>() {
                    @Override
                    public void onSubscribe(@NonNull ObservableSubscriber<Object> subscriber) {
                        looperFinallyNotNull.set(Looper.myLooper() != null);
                    }
                }).subscribe();
                latch.countDown();
            }
        });
        latch.await();
        assertTrue("Looper should initially be null", looperInitiallyNull.get());
        assertTrue("Looper should be initialized by observable class", looperFinallyNotNull.get());
    }

    @Test
    public void testObservableSubscriberIsUnsubscribed() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch onNextLatch = new CountDownLatch(1);
        final CountDownLatch onFinalLatch = new CountDownLatch(1);
        final Assertion<Boolean> unsubscribed = new Assertion<>(false);
        final List<String> list = new ArrayList<>();
        Subscription subscription = Observable.create(new ObservableAction<String>() {

            @Override
            public void onSubscribe(@NonNull ObservableSubscriber<String> subscriber) {
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext("test 1");
                }
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // should be unsubscribed after the latch countdown occurs
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext("test 2");
                }
                unsubscribed.set(subscriber.isUnsubscribed());
                onFinalLatch.countDown();
            }
        }).subscribeOn(Schedulers.newSingleThreadedScheduler())
            .observeOn(Schedulers.newSingleThreadedScheduler())
            .subscribe(new ObservableOnSubscribe<String>() {
                @Override
                public void onNext(@Nullable String item) {
                    list.add(item);
                    onNextLatch.countDown();
                }
            });

        onNextLatch.await();
        subscription.unsubscribe();
        latch.countDown();
        onFinalLatch.await();

        assertTrue("Only one item should have been emitted", list.size() == 1);
        assertTrue("Wrong item emitted", list.get(0).equals("test 1"));
        assertTrue("isUnsubscribed() was not correct", unsubscribed.get());
    }

    @Test
    public void testObservableEmpty_emitsNothingImmediately() throws Exception {
        final Assertion<Boolean> onNextAssertion = new Assertion<>(false);
        final Assertion<Boolean> onCompleteAssertion = new Assertion<>(false);
        Observable.empty().subscribe(new ObservableOnSubscribe<Object>() {

            @Override
            public void onNext(@Nullable Object item) {
                onNextAssertion.set(true);
            }

            @Override
            public void onComplete() {
                onCompleteAssertion.set(true);
            }

        });

        assertFalse(onNextAssertion.get());
        assertTrue(onCompleteAssertion.get());
    }

}