package com.asamm.locus.addon.wearables.utils;

/**
 * Created by menion on 07/08/15.
 * Asamm Software, s. r. o.
 */
public class Const {

	// minimal version code of Locus
	public static final int LOCUS_VERSION_CODE = 409;

	// scheme for intent filter
	private static final String PATH_SCHEME = "wear";
	// host for intent filter
	private static final String PATH_HOST = "locus_map";
	// path prefix for intent filter
	private static final String PATH_PREFIX = "/wearable";
	// completed base path
//	private static final String BASE_PATH =
//			PATH_SCHEME + "://" + PATH_HOST + PATH_PREFIX;
	private static final String BASE_PATH = "/locus_map/wearable";

	// PUBLIC PARAMETERS

	public static final String PATH_GET_BASE_DATA =
			BASE_PATH + "/get_base_data";
	public static final String PATH_GET_TRACK_RECORD_PROFILES =
			BASE_PATH + "/get_track_record_profiles";
	public static final String PATH_GET_MAP_PREVIEW =
			BASE_PATH + "/get_map_preview/";

	public static final String PATH_LOADED_DATA_CONTAINER =
			BASE_PATH + "/loaded_data_container";

	public static final String PATH_GET_PERIODIC_UPDATE =
			BASE_PATH + "/get_periodic_update";
	public static final String PATH_LOADED_PERIODIC_UPDATE =
			BASE_PATH + "/loaded_periodic_update";

	public static final String PATH_STATE_APP_DESTROYED =
			BASE_PATH + "/state_app_destroyed";

	public static final String PATH_TRACK_REC_START =
			BASE_PATH + "/track_record/start/";
	public static final String PATH_TRACK_REC_STOP =
			BASE_PATH + "/track_record/stop";
	public static final String PATH_TRACK_REC_PAUSE =
			BASE_PATH + "/track_record/pause";
	public static final String PATH_TRACK_REC_ADD_WPT =
			BASE_PATH + "/track_record/add_wpt";
}
