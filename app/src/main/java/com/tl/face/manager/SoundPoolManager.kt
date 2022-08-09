package com.tl.face.manager

import android.content.Context
import android.media.SoundPool
import com.tl.face.R

/**
 * sound play
 **/
object SoundPoolManager {
  private lateinit var soundPool: SoundPool
  private lateinit var soundMap : HashMap<SoundType, Int>
  private var lastPlayId: Int = -1

  fun initHelper(context: Context) {
    soundPool = SoundPool.Builder().setMaxStreams(10).build()
    loadSound2Map(context)
  }
  private fun loadSound2Map(context: Context) {
    soundMap = HashMap()
    soundMap[SoundType.Pass] = soundPool.load(context, R.raw.raw_pass, 1)
    soundMap[SoundType.Failed] = soundPool.load(context, R.raw.raw_fail, 1)
  }

  fun playSound(type: SoundType) {
    stopPlay()
    soundMap?.let {
      if (it[type] != null) {
        lastPlayId = soundPool?.play(it[type]!!, 1F, 1F, 0, 0, 1F)
      }
    }
  }

  fun stopPlay() {
    if (lastPlayId != -1) {
      soundPool?.stop(lastPlayId)
    }
  }
}

enum class SoundType {
  Failed,
  Pass
}