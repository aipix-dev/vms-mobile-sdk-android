package com.mobile.vms.network

import com.mobile.vms.models.*
import com.mobile.vms.player.helpers.EN
import io.reactivex.Observable
import okhttp3.*
import retrofit2.Response
import retrofit2.http.*

interface ApiClientObservable {

	@GET(DICTIONARY)
	fun getTranslations(
		@Query(LANGUAGE) language: String? = EN,
		@Query(REVISION) revision: Int
	): Observable<VMSTranslations>

	@PUT(DEVICE)
	fun sendFcmToken(@Body fcmRequest: VMSFcmRequest): Observable<Response<ResponseBody>>

	@PUT(DEVICE)
	fun sendHuaweiToken(@Body fcmRequest: VMSHuaweiRequest): Observable<Response<ResponseBody>>

	@POST(TOKEN)
	fun login(@Body loginRequest: VMSLoginRequest): Observable<VMSLoginResponse>

	@POST(LOGOUT)
	fun logout(@Body logoutRequest: VMSLogoutRequest): Observable<ResponseBody>

	@POST(LOGOUT)
	fun logout(): Observable<ResponseBody>

	@GET(EXTERNAL_AUTH_URL)
	fun authUrlExternal(): Observable<VMSExternalAuthUrlResponse>

	@POST(EXTERNAL_AUTH_CALLBACK)
	fun loginByExternalUrl(@Body request: VMSExternalAuthCodeRequest): Observable<VMSLoginResponse>

	@POST(EXTERNAL_AUTH_CALLBACK)
	fun loginByExternalUrlSession(@Body request: VMSExternalAuthCodeSessionRequest): Observable<VMSLoginResponse>

	@GET(FLAT_TREE)
	fun getSearchTree(@Query(SEARCH) search: String): Observable<List<VMSCamera>>

	@GET(FLAT_TREE)
	fun getCamerasTree(): Observable<List<VMSCameraTree>>

	@POST(CAMERAS_RENAME)
	fun renameCamera(
		@Path(CAMERA) id: String,
		@Body request: VMSRenameCameraRequest
	): Observable<VMSCamera>

	@POST(FAVORITES_CRUD)
	fun createFavorite(@Path(CAMERA) id: String): Observable<VMSCamera>

	@DELETE(FAVORITES_CRUD)
	fun deleteFavorite(@Path(CAMERA) id: String): Observable<VMSStatusResponse>

	@GET(FAVORITES)
	fun getFavorites(@Query(PAGE) page: String? = "1"): Observable<VMSPaginatedResponse<VMSCamera>>

	@GET(GROUPS)
	fun getGroups(@Query(PAGE) page: String? = "1"): Observable<VMSPaginatedResponse<VMSChildGroup>>

	@PUT(GROUP_CRUD)
	fun updateGroup(
		@Path(ID) id: String,
		@Body group: VMSUpdateGroupRequest
	): Observable<VMSChildGroup>

	@DELETE(GROUP_CRUD)
	fun deleteGroup(@Path(ID) id: String): Observable<VMSStatusResponse>

	@PUT(GROUP_CRUD)
	fun renameGroup(@Path(ID) id: String, @Body name: VMSSimpleName): Observable<VMSChildGroup>

	@POST(GROUPS)
	fun createGroup(@Body name: VMSSimpleName): Observable<VMSCreateGroupResponse>

	@POST(GROUPS_SYNC)
	fun syncGroups(@Path(ID) id: String, @Body groups: VMSGroupSyncRequest): Observable<VMSGroupSync>

	@GET(STREAMS)
	fun getStreams(
		@Path(ID) id: String,
		@Query(TYPE) type: String? = "high",
		@Query(SOURCE) source: String? = "rtsp"
	): Observable<VMSStreamsResponse>

	@GET(ARCHIVE)
	fun getArchive(
		@Path(ID) id: String,
		@Query(START) start: String,
		@Query(SOURCE) source: String? = "rtsp"
	): Observable<VMSStreamsResponse>

	@GET(CAMERAS_PREVIEW)
	fun getCamerasPreview(
		@Path(ID) id: String,
		@Query(DATE) date: String? = null
	): Observable<VMSUrlPreviewResponse>

	@POST(CAMERAS_MOVE)
	fun moveCamera(@Path(ID) id: String, @Body ptz: Any): Observable<VMSStatusResponse>

	@POST(CAMERAS_MOVE_HOME)
	fun moveCameraHome(@Path(ID) id: String): Observable<VMSStatusResponse>

	@GET(SESSIONS_LIST)
	fun getSessions(): Observable<ArrayList<VMSSession>>

	@POST(SESSIONS_ID)
	fun deleteSession(@Path(ID) id: String): Observable<Response<Unit>>

	@GET(CAMERAS_INFO)
	fun getCamera(@Path(ID) id: String): Observable<VMSCamera>

	@GET(USER_SELF)
	fun getUser(): Observable<VMSUser>

	@GET(STATIC)
	fun getStatics(): Observable<VMSStatics>

	@GET(STATIC_BASIC)
	fun getBasicStatic(): Observable<VMSBasicStatic>

	// There are 20 attempts to send a request getCaptcha() from one IP address within 10 minutes.
	@GET(CAPTCHA)
	fun getCaptcha(): Observable<VMSCaptcha>

	@POST(CAMERAS_ISSUE)
	fun sendReport(@Path(ISSUE_KEY) key: String, @Path(ID) id: String): Observable<ResponseBody>

	@GET(CAMERAS_EVENTS)
	fun getCameraEvents(
		@Path(ID) cameraId: String,
		@Query(FROM) from: String,
		@Query(TO) to: String,
		@Query(TYPES_ARRAY) types: List<String>?
	): Observable<List<VMSEvent>>

	@GET(CAMERAS_NEAREST_EVENT)
	fun getNearestEvent(
		@Path(ID) cameraId: String,
		@Query(FROM) from: String,
		@Query(TYPES_ARRAY) types: List<String>,
		@Query(REWIND) direction: String
	): Observable<VMSNearestEvent>

	@POST(CAMERAS_EVENTS)
	fun createEvent(
		@Path(ID) cameraId: String,
		@Body createData: VMSEventCreateData
	): Observable<VMSEvent>

	@PUT(CAMERAS_ACCESS_MARK)
	fun updateEvent(
		@Path(ID) cameraId: String,
		@Path(MARK_ID) eventId: String,
		@Body createData: VMSEventCreateData
	): Observable<VMSEvent>

	@DELETE(CAMERAS_ACCESS_MARK)
	fun deleteEvent(
		@Path(ID) cameraId: String,
		@Path(MARK_ID) eventId: String
	): Observable<ResponseBody>

	@GET(WS_URL)
	fun getSocketUrl(): Observable<VMSSocketResponse>

	@GET(MARKS)
	fun getEventsMarks(
		@Query(PAGE) page: String? = "1",
		@Query(CAMERAS_ARRAY) cameras: List<Int>? = null,
		@Query(SORT) sort: String = "created_at",
		@Query(DIR) dir: String = "desc",
		@Query(DATE) date: String? = null,
		@Query(TIMEZONE) timezone: String? = null,
		@Query(FROM) from: String? = null,
		@Query(TO) to: String? = null,
	): Observable<VMSPaginatedResponse<VMSEvent>>

	@GET(EVENTS)
	fun getEventsSystem(
		@Query(PAGE) page: String? = "1",
		@Query(CAMERAS_ARRAY) cameras: List<Int>? = null,
		@Query(SORT) sort: String = "created_at",
		@Query(DIR) dir: String = "desc",
		@Query(DATE) date: String? = null,
		@Query(TIMEZONE) timezone: String? = null,
		@Query(FROM) from: String? = null,
		@Query(TO) to: String? = null,
	): Observable<VMSPaginatedResponse<VMSEvent>>

	@GET(ANALYTIC_CASE_EVENTS)
	fun getEventsAnalytic(
		@Query(PAGE) page: String? = "1",
		@Query(EVENTS_ARRAY) events: List<String>? = null,
		@Query(ANALYTIC_TYPES) types: List<String>? = null,
		@Query(CAMERAS_ARRAY) cameras: List<Int>? = null,
		@Query(IDS_ARRAY) cases: List<Int>? = null,
		@Query(DIR) dir: String = "desc",
		@Query(DATE) date: String? = null,
		@Query(TIMEZONE) timezone: String? = null,
		@Query(FROM) from: String? = null,
		@Query(TO) to: String? = null
	): Observable<VMSPaginatedResponse<VMSEvent>>

	@GET(INTERCOM_EVENTS)
	fun getIntercomEvents(
		@Query(PAGE) page: String? = "1",
		@Query(DATE) date: String? = null,
		@Query(TIMEZONE) timezone: String? = null,
		@Query(FROM) from: String? = null,
		@Query(TO) to: String? = null,
	): Observable<VMSPaginatedResponse<VMSEvent>>

	@GET(ANALYTIC_CASE)
	fun getEventsAnalyticCases(@Query(TYPES_ARRAY) types: List<String>): Observable<VMSPaginatedResponse<VMSAnalyticCase>>

	@PUT(USER_SELF)
	fun savePassword(@Body group: VMSChangePasswordRequest): Observable<ResponseBody>

	@POST(CALLS_ANSWER)
	fun callAnswered(@Path(ID) id: String): Observable<VMSIntercomAnswer>

	@POST(CALLS_CANCEL)
	fun callCanceled(@Path(ID) id: String): Observable<Response<Unit>>

	@POST(CALLS_END)
	fun callEnded(@Path(ID) id: String): Observable<Response<Unit>>

	@POST(INTERCOM_OPEN_DOOR)
	fun openDoor(@Path(ID) id: String): Observable<Response<Unit>>

	@GET(INTERCOM)
	fun getIntercomsList(@Query(PAGE) page: String? = "1"): Observable<VMSPaginatedResponse<VMSIntercom>>

	@HTTP(method = DELETE, path = INTERCOM, hasBody = true)
	fun deleteIntercoms(@Body data: VMSIntercomsDeleteData): Observable<Response<Unit>>

	@PATCH(INTERCOM_PATCH)
	fun renameIntercom(
		@Path(ID) id: String,
		@Body data: VMSIntercomChangeTitleData
	): Observable<VMSIntercom>

	@PATCH(INTERCOM_PATCH)
	fun changeIntercomSettings(
		@Path(ID) id: String,
		@Body data: VMSIntercomSettingsDataEnabled
	): Observable<VMSIntercom>

	@GET(INTERCOM_CODES)
	fun getIntercomCodesList(@Query(PAGE) page: String? = "1"): Observable<VMSPaginatedResponse<VMSCodeVisitor>>

	@HTTP(method = DELETE, path = INTERCOM_CODES, hasBody = true)
	fun deleteVisitors(@Body data: VMSIntercomsDeleteData): Observable<Response<Unit>>

	@POST(INTERCOM_ID_CODES)
	fun createCode(
		@Path(ID) id: String,
		@Body data: VMSIntercomCodeData
	): Observable<VMSCodeVisitor>

	@GET(INTERCOM_CALLS)
	fun getCallsList(@Query(PAGE) page: String? = "1"): Observable<VMSPaginatedResponse<VMSIntercomCall>>

	@HTTP(method = DELETE, path = INTERCOM_CALLS, hasBody = true)
	fun deleteCalls(@Body data: VMSIntercomsDeleteData): Observable<Response<Unit>>

	@POST(INTERCOM)
	fun getActivationCode(): Observable<VMSActivationCode>

	@POST(INTERCOM_FLAT)
	fun setIntercomFlat(
		@Path(ID) id: String,
		@Body data: VMSIntercomFlatData
	): Observable<VMSIntercom>

	@GET(CAMERAS_ARCHIVE_LINK)
	fun getArchiveLink(
		@Path(ID) id: String,
		@Query(FROM) from: String,
		@Query(TO) to: String
	): Observable<VMSStatusResponse>

	@GET(CAMERAS_EXISTED)
	fun getWidgetCameras(@Query(IDS_ARRAY) shows: ArrayList<Int>): Observable<ArrayList<VMSCamera>>

	@GET(INTERCOM_EXISTED)
	fun getWidgetIntercoms(@Query(IDS_ARRAY) shows: ArrayList<Int>): Observable<ArrayList<VMSIntercom>>

	@GET(CALLS_STATUS)
	fun callStatus(@Path(ID) id: String): Observable<VMSIntercomCall>

	@GET(CAMERAS_WITH_ANALYTICS)
	fun getCamerasList(
		@Query(SEARCH) search: String? = null,
		@Query(FILTER) filter: String = "analytics",
		@Query(PAGE) page: String? = "1"
	): Observable<VMSPaginatedResponse<VMSCamera>>

	@GET
	fun getCamerasList(@Url url: String): Observable<VMSPaginatedResponse<VMSCamera>>

	@PUT(USER_SELF)
	fun changeLanguage(@Body language: VMSLanguageName): Observable<ResponseBody>

	@GET(INTERCOM_FILES)
	fun getAnalyticsFiles(
		@Path(INTERCOM_PATH) intercom: String,
		@Query(PAGE) page: String? = "1",
	): Observable<VMSPaginatedResponse<VMSAnalyticFile>>

	@Multipart
	@POST(INTERCOM_FILES)
	fun createAnalyticFile(
		@Path(INTERCOM_PATH) intercom: String,
		@Part image: MultipartBody.Part,
		@Part(NAME) name: String,
		@Part(TYPE) type: String = "face_resource",
	): Observable<VMSAnalyticFile>

	@POST(INTERCOM_FILE_RENAME)
	fun updateAnalyticFile(
		@Path(INTERCOM_PATH) intercom: String,
		@Path(FILE_PATH) file: String,
		@Body data: IntercomAnalyticFileName,
	): Observable<VMSAnalyticFile>

	@HTTP(method = "DELETE", path = INTERCOM_FILES, hasBody = true)
	fun deleteAnalyticFile(
		@Path(INTERCOM_PATH) intercom: String,
		@Body body: IntercomAnalyticFileData,
	): Observable<VMSAnalyticFile>

	@POST(BRIDGES)
	fun addBridge(@Body body: VMSAddBridgeRequest): Observable<VMSBridge>

	@PATCH(BRIDGE)
	fun renameBridge(
		@Path(BRIDGE_ID) id: String,
		@Body body: VMSRenameBridgeRequest
	): Observable<VMSBridge>

	@GET(BRIDGE)
	fun getBridge(@Path(BRIDGE_ID) id: String): Observable<VMSBridge>

	@GET(BRIDGES)
	fun getBridges(@Query(PAGE) page: String): Observable<VMSPaginatedResponse<VMSBridge>>

	@GET(BRIDGE_CAMERAS_CONNECTED)
	fun getBridgeCamerasConnected(
		@Path(BRIDGE_ID) id: String,
		@Query(PAGE) page: String
	): Observable<VMSPaginatedResponse<VMSCamera>>

	@HTTP(method = "DELETE", path = BRIDGE_REMOVE_CAMERA, hasBody = false)
	fun removeBridgeCamera(
		@Path(BRIDGE_ID) bridgeId: String,
		@Path(CAMERA) cameraId: String
	): Observable<Response<Unit>>

	@HTTP(method = "DELETE", path = BRIDGE, hasBody = false)
	fun deleteBridge(@Path(BRIDGE_ID) id: String): Observable<Response<Unit>>

}