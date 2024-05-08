package com.mobile.vms.network

import com.mobile.vms.models.*
import com.mobile.vms.player.helpers.EN
import okhttp3.*
import retrofit2.Response
import retrofit2.http.*

interface ApiClientCoroutines {

	@GET(DICTIONARY)
	suspend fun getTranslations(
		@Query(LANGUAGE) language: String? = EN,
		@Query(REVISION) revision: Int
	): VMSTranslations

	@PUT(DEVICE)
	suspend fun sendFcmToken(@Body fcmRequest: VMSFcmRequest): Response<ResponseBody>

	@PUT(DEVICE)
	suspend fun sendHuaweiToken(@Body hmsRequest: VMSHuaweiRequest): Response<ResponseBody>

	@POST(TOKEN)
	suspend fun login(@Body loginRequest: VMSLoginRequest): VMSLoginResponse

	@POST(LOGOUT)
	suspend fun logout(@Body logoutRequest: VMSLogoutRequest): ResponseBody

	@POST(LOGOUT)
	suspend fun logout(): ResponseBody

	@GET(EXTERNAL_AUTH_URL)
	suspend fun authUrlExternal(): VMSExternalAuthUrlResponse

	@POST(EXTERNAL_AUTH_CALLBACK)
	suspend fun loginByExternalUrl(@Body request: VMSExternalAuthCodeRequest): VMSLoginResponse

	@POST(EXTERNAL_AUTH_CALLBACK)
	suspend fun loginByExternalUrlSession(@Body request: VMSExternalAuthCodeSessionRequest): VMSLoginResponse

	@GET(FLAT_TREE)
	suspend fun getSearchTree(@Query(SEARCH) search: String): List<VMSCamera>

	@GET(FLAT_TREE)
	suspend fun getCamerasTree(): List<VMSCameraTree>

	@POST(CAMERAS_RENAME)
	suspend fun renameCamera(
		@Path(CAMERA) id: String,
		@Body request: VMSRenameCameraRequest
	): VMSCamera

	@POST(FAVORITES_CRUD)
	suspend fun createFavorite(@Path(CAMERA) id: String): VMSCamera

	@DELETE(FAVORITES_CRUD)
	suspend fun deleteFavorite(@Path(CAMERA) id: String): VMSStatusResponse

	@GET(FAVORITES)
	suspend fun getFavorites(@Query(PAGE) page: String? = "1"): VMSPaginatedResponse<VMSCamera>

	@GET(GROUPS)
	suspend fun getGroups(@Query(PAGE) page: String? = "1"): VMSPaginatedResponse<VMSChildGroup>

	@PUT(GROUP_CRUD)
	suspend fun updateGroup(@Path(ID) id: String, @Body group: VMSUpdateGroupRequest): VMSChildGroup

	@DELETE(GROUP_CRUD)
	suspend fun deleteGroup(@Path(ID) id: String): VMSStatusResponse

	@PUT(GROUP_CRUD)
	suspend fun renameGroup(@Path(ID) id: String, @Body name: VMSSimpleName): VMSChildGroup

	@POST(GROUPS)
	suspend fun createGroup(@Body name: VMSSimpleName): VMSCreateGroupResponse

	@POST(GROUPS_SYNC)
	suspend fun syncGroups(@Path(ID) id: String, @Body groups: VMSGroupSyncRequest): VMSGroupSync

	@GET(STREAMS)
	suspend fun getStreams(
		@Path(ID) id: String,
		@Query(TYPE) type: String? = "high",
		@Query(SOURCE) source: String? = "hls"
	): VMSStreamsResponse

	@GET(ARCHIVE)
	suspend fun getArchive(
		@Path(ID) id: String,
		@Query(START) start: String,
	): VMSStreamsResponse

	@GET(CAMERAS_PREVIEW)
	suspend fun getCamerasPreview(
		@Path(ID) id: String,
		@Query(DATE) date: String? = null
	): VMSUrlPreviewResponse

	@POST(CAMERAS_MOVE)
	suspend fun moveCamera(@Path(ID) id: String, @Body ptz: Any): VMSStatusResponse

	@POST(CAMERAS_MOVE_HOME)
	suspend fun moveCameraHome(@Path(ID) id: String): VMSStatusResponse

	@GET(SESSIONS_LIST)
	suspend fun getSessions(): ArrayList<VMSSession>

	@POST(SESSIONS_ID)
	suspend fun deleteSession(@Path(ID) id: String): Response<Unit>

	@GET(CAMERAS_INFO)
	suspend fun getCamera(@Path(ID) id: String): VMSCamera

	@GET(USER_SELF)
	suspend fun getUser(): VMSUser

	@GET(STATIC)
	suspend fun getStatics(): VMSStatics

	@GET(STATIC_BASIC)
	suspend fun getBasicStatic(): VMSBasicStatic

	// There are 20 attempts to send a request getCaptcha() from one IP address within 10 minutes.
	@GET(CAPTCHA)
	suspend fun getCaptcha(): VMSCaptcha

	@POST(CAMERAS_ISSUE)
	suspend fun sendReport(@Path(ISSUE_KEY) key: String, @Path(ID) id: String): ResponseBody

	@GET(CAMERAS_EVENTS)
	suspend fun getCameraEvents(
		@Path(ID) cameraId: String,
		@Query(FROM) from: String,
		@Query(TO) to: String,
		@Query(TYPES_ARRAY) types: List<String>
	): List<VMSEvent>

	@GET(CAMERAS_NEAREST_EVENT)
	suspend fun getNearestEvent(
		@Path(ID) cameraId: String,
		@Query(FROM) from: String,
		@Query(TYPES_ARRAY) types: List<String>,
		@Query(REWIND) direction: String
	): VMSNearestEvent

	@POST(CAMERAS_EVENTS)
	suspend fun createEvent(
		@Path(ID) cameraId: String,
		@Body createData: VMSEventCreateData
	): VMSEvent

	@PUT(CAMERAS_ACCESS_MARK)
	suspend fun updateEvent(
		@Path(ID) cameraId: String,
		@Path(MARK_ID) eventId: String,
		@Body createData: VMSEventCreateData
	): VMSEvent

	@DELETE(CAMERAS_ACCESS_MARK)
	suspend fun deleteEvent(
		@Path(ID) cameraId: String,
		@Path(MARK_ID) eventId: String
	): ResponseBody

	@GET(WS_URL)
	suspend fun getSocketUrl(): VMSSocketResponse

	@GET(MARKS)
	suspend fun getEventsMarks(
		@Query(PAGE) page: String? = "1",
		@Query(CAMERAS_ARRAY) cameras: List<Int>? = null,
		@Query(SORT) sort: String,
		@Query(DIR) dir: String,
		@Query(DATE) date: String? = null,
		@Query(TIMEZONE) timezone: String? = null,
		@Query(FROM) from: String? = null,
		@Query(TO) to: String? = null,
	): VMSPaginatedResponse<VMSEvent>

	@GET(EVENTS)
	suspend fun getEventsSystem(
		@Query(PAGE) page: String? = "1",
		@Query(CAMERAS_ARRAY) cameras: List<Int>? = null,
		@Query(SORT) sort: String,
		@Query(DIR) dir: String,
		@Query(DATE) date: String? = null,
		@Query(TIMEZONE) timezone: String? = null,
		@Query(FROM) from: String? = null,
		@Query(TO) to: String? = null,
	): VMSPaginatedResponse<VMSEvent>

	@GET(ANALYTIC_CASE_EVENTS)
	suspend fun getEventsAnalytic(
		@Query(PAGE) page: String? = "1",
		@Query(EVENTS_ARRAY) events: List<String>? = null,
		@Query(ANALYTIC_TYPES) types: List<String>? = null,
		@Query(CAMERAS_ARRAY) cameras: List<Int>? = null,
		@Query(IDS_ARRAY) cases: List<Int>? = null,
		@Query(DIR) dir: String,
		@Query(DATE) date: String? = null,
		@Query(TIMEZONE) timezone: String? = null,
		@Query(FROM) from: String? = null,
		@Query(TO) to: String? = null,
	): VMSPaginatedResponse<VMSEvent>

	@GET(INTERCOM_EVENTS)
	suspend fun getIntercomEvents(@Query(PAGE) page: String? = "1"): VMSPaginatedResponse<VMSEvent>

	@GET(ANALYTIC_CASE)
	suspend fun getEventsAnalyticCases(@Query(TYPES_ARRAY) types: List<String>): VMSPaginatedResponse<VMSAnalyticCase>

	@PUT(USER_SELF)
	suspend fun savePassword(@Body group: VMSChangePasswordRequest): ResponseBody

	@POST(CALLS_ANSWER)
	suspend fun callAnswered(@Path(ID) id: String): VMSIntercomAnswer

	@POST(CALLS_CANCEL)
	suspend fun callCanceled(@Path(ID) id: String): Response<Unit>

	@POST(CALLS_END)
	suspend fun callEnded(@Path(ID) id: String): Response<Unit>

	@POST(INTERCOM_OPEN_DOOR)
	suspend fun openDoor(@Path(ID) id: String): Response<Unit>

	@GET(INTERCOM)
	suspend fun getIntercomsList(@Query(PAGE) page: String? = "1"): VMSPaginatedResponse<VMSIntercom>

	@HTTP(method = DELETE, path = INTERCOM, hasBody = true)
	suspend fun deleteIntercoms(@Body data: VMSIntercomsDeleteData): Response<Unit>

	@PATCH(INTERCOM_PATCH)
	suspend fun renameIntercom(
		@Path(ID) id: String,
		@Body data: VMSIntercomChangeTitleData
	): VMSIntercom

	@PATCH(INTERCOM_PATCH)
	suspend fun changeIntercomSettings(
		@Path(ID) id: String,
		@Body data: VMSIntercomSettingsDataEnabled
	): VMSIntercom

	@GET(INTERCOM_CODES)
	suspend fun getIntercomCodesList(@Query(PAGE) page: String? = "1"): VMSPaginatedResponse<VMSCodeVisitor>


	@HTTP(method = DELETE, path = INTERCOM_CODES, hasBody = true)
	suspend fun deleteVisitors(@Body data: VMSIntercomsDeleteData): Response<Unit>

	@POST(INTERCOM_ID_CODES)
	suspend fun createCode(
		@Path(ID) id: String,
		@Body data: VMSIntercomCodeData
	): VMSCodeVisitor

	@GET(INTERCOM_CALLS)
	suspend fun getCallsList(@Query(PAGE) page: String? = "1"): VMSPaginatedResponse<VMSIntercomCall>

	@HTTP(method = DELETE, path = INTERCOM_CALLS, hasBody = true)
	suspend fun deleteCalls(@Body data: VMSIntercomsDeleteData): Response<Unit>

	@POST(INTERCOM)
	suspend fun getActivationCode(): VMSActivationCode

	@POST(INTERCOM_FLAT)
	suspend fun setIntercomFlat(@Path(ID) id: String, @Body data: VMSIntercomFlatData): VMSIntercom

	@GET(CAMERAS_ARCHIVE_LINK)
	suspend fun getArchiveLink(
		@Path(ID) id: String,
		@Query(FROM) from: String,
		@Query(TO) to: String
	): VMSStatusResponse

	@GET(CAMERAS_EXISTED)
	suspend fun getWidgetCameras(@Query(IDS_ARRAY) shows: ArrayList<Int>): ArrayList<VMSCamera>

	@GET(INTERCOM_EXISTED)
	suspend fun getWidgetIntercoms(@Query(IDS_ARRAY) shows: ArrayList<Int>): ArrayList<VMSIntercom>

	@GET(CALLS_STATUS)
	suspend fun callStatus(@Path(ID) id: String): VMSIntercomCall

	@GET(CAMERAS_WITH_ANALYTICS)
	suspend fun getCamerasList(
		@Query(SEARCH) search: String? = null,
		@Query(FILTER) filter: String = "analytics",
		@Query(PAGE) page: String? = "1"
	): VMSPaginatedResponse<VMSCamera>

	@GET
	suspend fun getCamerasList(@Url url: String): VMSPaginatedResponse<VMSCamera>

	@PUT(USER_SELF)
	suspend fun changeLanguage(@Body language: VMSLanguageName): ResponseBody

	@GET(INTERCOM_FILES)
	suspend fun getAnalyticsFiles(
		@Path(INTERCOM_PATH) intercom: String,
		@Query(PAGE) page: String? = "1",
	): VMSPaginatedResponse<VMSAnalyticFile>

	@Multipart
	@POST(INTERCOM_FILES)
	suspend fun createAnalyticFile(
		@Path(INTERCOM_PATH) intercom: String,
		@Part image: MultipartBody.Part,
		@Part(NAME) name: String,
		@Part(TYPE) type: String = "face_resource",
	): VMSAnalyticFile

	@POST(ANALYTIC_GROUP_FILES)
	suspend fun updateAnalyticFile(
		@Path(INTERCOM_PATH) intercom: String,
		@Path(FILE_PATH) file: String,
		@Body data: IntercomAnalyticFileName,
	): VMSAnalyticFile

	@HTTP(method = "DELETE", path = INTERCOM_FILES, hasBody = true)
	suspend fun deleteAnalyticFile(
		@Path(INTERCOM_PATH) intercom: String,
		@Body body: IntercomAnalyticFileData,
	): VMSAnalyticFile

	@POST(BRIDGES)
	suspend fun addBridge(@Body body: VMSAddBridgeRequest): VMSBridge

	@PATCH(BRIDGE)
	suspend fun renameBridge(
		@Path(BRIDGE_ID) id: String,
		@Body body: VMSRenameBridgeRequest
	): VMSBridge

	@GET(BRIDGE)
	suspend fun getBridge(@Path(BRIDGE_ID) id: String): VMSBridge

	@GET(BRIDGES)
	suspend fun getBridges(@Query(PAGE) page: String): VMSPaginatedResponse<VMSBridge>

	@GET(BRIDGE_CAMERAS_CONNECTED)
	suspend fun getBridgeCamerasConnected(
		@Path(BRIDGE_ID) id: String,
		@Query(PAGE) page: String
	): VMSPaginatedResponse<VMSCamera>

	@HTTP(method = "DELETE", path = BRIDGE_REMOVE_CAMERA, hasBody = false)
	suspend fun removeBridgeCamera(
		@Path(BRIDGE_ID) bridgeId: String,
		@Path(CAMERA) cameraId: String
	): Response<Unit>

	@HTTP(method = "DELETE", path = BRIDGE, hasBody = false)
	suspend fun deleteBridge(@Path(BRIDGE_ID) id: String): Response<Unit>

}