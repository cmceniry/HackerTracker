package com.shortstack.hackertracker.utils

import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit

class TickTimer {

    private val interval: Observable<Long> = Observable.interval(0, 15, TimeUnit.SECONDS)
    private val subject = BehaviorSubject.create<Long>()
    private var disposable: Disposable? = null

    val observable: Observable<Long>
        get() = subject

    init {
        subject.onNext(0)
    }

    fun start() {
        disposable = interval.subscribe {
            subject.onNext(0)
        }
    }

    fun stop() {
        disposable?.dispose()
        disposable = null
    }
}