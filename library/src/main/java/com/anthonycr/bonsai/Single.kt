package com.anthonycr.bonsai

import com.anthonycr.bonsai.refactor.ReactiveEventException
import com.anthonycr.bonsai.refactor.requireCondition

/**
 * A Reactive Streams Kotlin implementation of a publisher that emits one item or one error. This
 * class allows work to be done on a certain thread and then allows an item to be emitted on a
 * different thread. It is a replacement for {@link android.os.AsyncTask}.
 * <p>
 * It allows the caller of this class to create a single task that emits a single item and then
 * completes, or if no item can be emitted, then an error must be emitted. The consumer of the
 * {@link Single} will be notified of the success of the subscription or the failure of it.
 *
 * @param <T> the type that the Single will emit.
 */
class Single<T> private constructor(private val onSubscribe: (Subscriber<T>) -> Unit) {

    companion object {
        @JvmStatic
        fun <R> error() = Single<R>({ it.onError(RuntimeException("No item emitted")) })

        @JvmStatic
        fun <R> create(block: (Subscriber<R>) -> Unit) = Single(block)

        @JvmStatic
        private fun <R> performSubscribe(subscriptionScheduler: Scheduler,
                                         observationScheduler: Scheduler,
                                         onSubscribe: (Subscriber<R>) -> Unit,
                                         onSuccess: (R) -> Unit,
                                         onError: (Throwable) -> Unit): Subscription {
            val composingSubscriber = ComposingSubscriber(onSuccess, onError)
            val schedulingSubscriber = SchedulingSubscriber(observationScheduler, composingSubscriber)
            subscriptionScheduler.execute {
                try {
                    onSubscribe(schedulingSubscriber)
                } catch (exception: Exception) {
                    if (exception is ReactiveEventException) {
                        throw exception
                    } else {
                        if (schedulingSubscriber.isUnsubscribed) {
                            throw ReactiveEventException("Exception thrown after unsubscribe", exception)
                        } else {
                            schedulingSubscriber.onError(exception)
                        }
                    }
                }
            }

            return schedulingSubscriber
        }
    }

    interface Consumer<in T> {
        fun onSuccess(t: T)

        fun onError(throwable: Throwable)
    }

    interface Subscriber<in T> : Consumer<T>, Subscription

    private class ComposingSubscriber<in T>(private var onSuccess: (T) -> Unit,
                                            private var onError: (Throwable) -> Unit) : Consumer<T> {
        override fun onSuccess(t: T) = onSuccess.invoke(t)

        override fun onError(throwable: Throwable) = onError.invoke(throwable)
    }

    private class SchedulingSubscriber<in T>(private val scheduler: Scheduler,
                                             private var composingSubscriber: ComposingSubscriber<T>?) : Subscriber<T> {

        private var onSuccessExecuted = false
        private var onErrorExecuted = false

        override fun unsubscribe() {
            composingSubscriber = null
        }

        override fun isUnsubscribed() = composingSubscriber == null

        override fun onSuccess(t: T) = scheduler.execute {
            requireCondition(!onSuccessExecuted) { "onSuccess must not be called multiple times" }
            requireCondition(!onErrorExecuted) { "onSuccess must not be called after onError" }
            onSuccessExecuted = true
            composingSubscriber?.onSuccess(t)
            unsubscribe()
        }

        override fun onError(throwable: Throwable) = scheduler.execute {
            requireCondition(!onErrorExecuted) { "onError must not be called multiple times" }
            requireCondition(!onSuccessExecuted) { "onError must not be called after onSuccess" }
            onErrorExecuted = true
            composingSubscriber?.onError(throwable)
            unsubscribe()
        }
    }

    private var subscriptionScheduler = Schedulers.immediate()
    private var observationScheduler = Schedulers.immediate()

    fun subscribeOn(scheduler: Scheduler): Single<T> {
        subscriptionScheduler = scheduler
        return this
    }

    fun observeOn(scheduler: Scheduler): Single<T> {
        observationScheduler = scheduler
        return this
    }

    fun <R> map(map: (T) -> R): Single<R> {
        return create<R>({ newOnSubscribe ->
            performSubscribe(
                    Schedulers.immediate(),
                    Schedulers.immediate(),
                    onSubscribe,
                    onSuccess = { newOnSubscribe.onSuccess(map(it)) },
                    onError = { newOnSubscribe.onError(it) }
            )
        }).subscribeOn(subscriptionScheduler)
                .observeOn(observationScheduler)
    }

    fun subscribe(onSuccess: (T) -> Unit = {},
                  onError: (Throwable) -> Unit = { throw ReactiveEventException("No error handler supplied", it) }) =
            performSubscribe(subscriptionScheduler, observationScheduler, onSubscribe, onSuccess, onError)

}