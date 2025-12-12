package com.stepfun.stepfunaudiocoresdk.audio.common.audio

interface AudioPlaybackCallback {

    /** 开始播放 */
    fun onPlaybackStarted()

    /** 暂停播放 */
    fun onPlaybackPaused()

    /** 恢复播放 */
    fun onPlaybackResumed()

    /** 停止播放 */
    fun onPlaybackStopped()

    /** 播放完成（队列中所有数据播放完毕） */
    fun onPlaybackCompleted()
}