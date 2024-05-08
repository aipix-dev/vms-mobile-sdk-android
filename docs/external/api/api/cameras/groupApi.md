@toc API/Requests/Cameras/GroupApi

# GroupApi #

API for manipulating groups of cameras.


## Get group list

Get list of camera groups. Specify the page for the request.

If the request was successful, you'll receive a response with a paginated list of groups.

```
@GET(GROUPS)
suspend fun getGroups(@Query(PAGE) page: String? = "1"): VMSPaginatedResponse<VMSChildGroup>
```


## Create group

Create a new group of cameras with the specified name. The group is initially empty.

If the request was successful, you'll get a `VMSCreateGroupResponse'.

```
@POST(GROUPS)
suspend fun createGroup(@Body name: VMSSimpleName): VMSCreateGroupResponse
```


## Rename group

Rename the specified group by it's id with the new name.

If the request was successful, you'll get a `VMSChildGroup`.

```
@PUT(GROUP_CRUD)
suspend fun renameGroup(@Path(ID) id: String, @Body name: VMSSimpleName): VMSChildGroup
```


## Delete group

Delete specific group by it's id.

If the request was successful, you'll get a `VMSStatusResponse`.

```
@DELETE(GROUP_CRUD)
suspend fun deleteGroup(@Path(ID) id: String): VMSStatusResponse
```


## Update group

Update group with specified information.

If the request was successful, you'll get a `VMSChildGroup`.

```
@PUT(GROUP_CRUD)
suspend fun updateGroup(@Path(ID) id: String, @Body group: VMSUpdateGroupRequest): VMSChildGroup
```

`id` - id of the group to specify the needed group


### VMSUpdateGroupRequest

Object containing the information needed to update a group.

`name` - new name of the group; if you don't want to change the group name - set the old name in this parameter

`items` -  list of camera ids you want to add to this group


## Sync groups

This request will synchronise the specified camera with all user groups.

The request requires a list of groups that this camera will belong to.

The camera will be removed from other groups.

If the request was successful, the response will be a `VMSGroupSync' object.

```
@POST(GROUPS_SYNC)
suspend fun syncGroups(@Path(ID) id: String, @Body groups: VMSGroupSyncRequest): VMSGroupSync
```

`id` - specify camera by it's id

`groups` - specify the list of group ids where the camera will be displayed (camera will be added to a group if it wasn't there before)


### VMSGroupSync

Information about server-side group synchronisation.

```
@Parcelize
data class VMSGroupSync(
	@SerializedName("type") val type: String = ""
): Parcelable
```

`sync` - you will get this type, if the synchronisation was done

`async` - you will get this type, if user has more than 50 group the backend request will run asynchronously

When the async process is complete, you'll receive a socket message that you can process. See `VMSPusherApi` for more details.
