package com.mobile.vms.player.helpers

import io.reactivex.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.*
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.Subject

fun <T> Observable<T>.subsIoObsMain(): Observable<T> {
	return this.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
}

fun <T> Single<T>.subsIoObsMain(): Single<T> {
	return this.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
}

fun Completable.subsIoObsMain(): Completable {
	return this.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
}

fun <T> Observable<T>.obsMain(): Observable<T> {
	return this.observeOn(AndroidSchedulers.mainThread())
}

fun <T> Observable<T>.obsIO(): Observable<T> {
	return this.observeOn(Schedulers.io())
}

fun Disposable.addTo(composite: CompositeDisposable) {
	composite.add(this)
}

fun Disposable.delete(composite: CompositeDisposable) {
	composite.delete(this)
}

fun <T> Observable<T>.delegateToSubject(subj: Subject<T>): Disposable {
	return this.subscribe({ subj.onNext(it!!) }, { subj.onError(it!!) }, { subj.onComplete() })
}