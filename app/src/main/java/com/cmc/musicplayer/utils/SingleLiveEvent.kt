package com.cmc.musicplayer.utils

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer

class SingleLiveEvent<T> : MutableLiveData<T>() {

    private val observers = mutableSetOf<Observer<in T>>()

    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        if (observers.add(observer)) {
            super.observe(owner, Observer { t ->
                if (t != null) {
                    observer.onChanged(t)
                    value = null
                }
            })
        }
    }
}
