package com.mobile.vms.player.rtsp

import java.io.IOException

/**
 * Handle for not available archive
 */
class RtspError204: IOException()

class RtspErrorEmptyStream(val needShowError: Boolean): IOException()

/**
 * SocketTimeoutException: Read timed out
 * For plan: todo handle for LIVE - when lost socket connection
 */
class RtspSocketTimeoutException: IOException()