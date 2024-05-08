package com.mobile.vms.network

/**
 * Base version is "v1", but version could be change for specific request
 */
const val API = "api/"
const val V1 = "v1/"
const val API_V1 = "$API$V1"

// Params
const val ID = "id"
const val TYPE = "type"
const val SOURCE = "source"
const val START = "start"
const val MARK_ID = "markId"
const val ISSUE_KEY = "issue_key"
const val CAMERA = "camera"
const val FROM = "from"
const val TO = "to"
const val REWIND = "rewind"
const val DELETE = "DELETE"
const val PAGE = "page"
const val SORT = "sort"
const val DIR = "dir"
const val DATE = "date"
const val TIMEZONE = "timezone"
const val SEARCH = "search"
const val FILTER = "filter"
const val LANGUAGE = "language"
const val REVISION = "revision"
const val NAME = "name"
const val EVENTS_ARRAY = "events[]"
const val CAMERAS_ARRAY = "cameras[]"
const val TYPES_ARRAY = "types[]"
const val IDS_ARRAY = "ids[]"
const val ANALYTIC_TYPES = "analytic_types[]"
const val INTERCOM_PATH = "intercom"
const val FILE_PATH = "file"
const val BRIDGE_ID = "bridge"

// Auth user
const val TOKEN = API_V1 + "token"
const val LOGOUT = API_V1 + "logout"
const val CAPTCHA = API_V1 + "captcha"
const val WS_URL = API_V1 + "wsurl"
const val EXTERNAL_AUTH_URL = API_V1 + "external/auth/url"
const val EXTERNAL_AUTH_CALLBACK = API_V1 + "external/auth/callback"
const val DEVICE = API_V1 + "devices"
const val SESSIONS_LIST = API_V1 + "sessions"
const val SESSIONS_ID = API_V1 + "sessions/{id}"
const val USER_SELF = API_V1 + "users/self"

// Static data
const val STATIC = API_V1 + "static"
const val STATIC_BASIC = API_V1 + "static/basic"
const val DICTIONARY = API_V1 + "dictionary"

// Cameras
const val FLAT_TREE = API_V1 + "cameras/flat-tree"
const val FAVORITES = API_V1 + "cameras/favorites"
const val FAVORITES_CRUD = API_V1 + "cameras/{camera}/favorites"
const val GROUPS = API_V1 + "groups"
const val GROUPS_SYNC = API_V1 + "cameras/{id}/sync-groups"
const val GROUP_CRUD = API_V1 + "groups/{id}"

// Camera
const val CAMERAS_PREVIEW = API_V1 + "cameras/{id}/preview"
const val CAMERAS_MOVE = API_V1 + "cameras/{id}/move"
const val CAMERAS_MOVE_HOME = API_V1 + "cameras/{id}/move/home"
const val CAMERAS_INFO = API_V1 + "cameras/{id}"
const val CAMERAS_EVENTS = API_V1 + "cameras/{id}/marks"
const val CAMERAS_NEAREST_EVENT = API_V1 + "cameras/{id}/marks/rewind"
const val CAMERAS_ACCESS_MARK = API_V1 + "cameras/{id}/marks/{markId}"
const val CAMERAS_ISSUE = API_V1 + "issues/{issue_key}/cameras/{id}"
const val CAMERAS_RENAME = API_V1 + "cameras/{camera}/rename"
const val CAMERAS_WITH_ANALYTICS = API_V1 + "cameras"

// Player
const val STREAMS = API_V1 + "cameras/{id}/streams"
const val ARCHIVE = API_V1 + "cameras/{id}/streams/archive"
const val CAMERAS_ARCHIVE_LINK = API_V1 + "cameras/{id}/archive/link"

// Events & Analytic
const val EVENTS = API_V1 + "events" // system events
const val MARKS = API_V1 + "marks" // user events
const val ANALYTIC_CASE_EVENTS = API_V1 + "analytic-case/events" // analytic events
const val ANALYTIC_CASE = API_V1 + "analytic-case"
const val ANALYTIC_GROUP_FILES = API_V1 + "analytics/files/{analytic_file}"

// Intercom
const val INTERCOM = API_V1 + "intercom"
const val INTERCOM_PATCH = API_V1 + "intercom/{id}"
const val INTERCOM_CODES = API_V1 + "intercom/codes"
const val INTERCOM_ID_CODES = API_V1 + "intercom/{id}/codes"
const val INTERCOM_EVENTS = API_V1 + "intercom/events"
const val INTERCOM_FLAT = API_V1 + "intercom/{id}/flat"
const val INTERCOM_FILES = API_V1 + "intercom/{intercom}/files" // list/creation/deleting files
const val INTERCOM_FILE_RENAME = API_V1 + "intercom/{intercom}/files/{file}"
const val INTERCOM_OPEN_DOOR = API_V1 + "intercom/{id}/open-door"

// Intercom calls
const val INTERCOM_CALLS = API_V1 + "intercom/calls"
const val CALLS_STATUS = API_V1 + "intercom/calls/{id}"
const val CALLS_ANSWER = API_V1 + "intercom/calls/{id}/start"
const val CALLS_CANCEL = API_V1 + "intercom/calls/{id}/cancel"
const val CALLS_END = API_V1 + "intercom/calls/{id}/end"

// Widgets
const val CAMERAS_EXISTED = API_V1 + "cameras/shows"
const val INTERCOM_EXISTED = API_V1 + "intercom/shows"

//Bridges
const val BRIDGES = API_V1 + "bridges"
const val BRIDGE = API_V1 + "bridges/{bridge}"
const val BRIDGE_CAMERAS_CONNECTED = API_V1 + "bridges/{bridge}/cameras/connected"
const val BRIDGE_REMOVE_CAMERA = API_V1 + "bridges/{bridge}/cameras/{camera}"
