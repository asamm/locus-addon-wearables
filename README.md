# Locus Map - add-on Wearables

Add-on for [Locus Map](http://www.locusmap.eu) (Android) application, focused on using most important features if Locus Map on Android Wear devices.

Add-on consists of two parts.

- `device` module - middleman who communicate with Locus Map (over Locus API) on one side and add-on running on watches (over messages) on second side
- `wear` module - separate application running on watches

### Setup project in Android Studio

- Android Studio > File > New project
  - create project for `Android 4.0+`
  - no activity etc. needed
- clone current repository in created directory by
  - `git clone https://github.com/asamm/locus-addon-wearables.git addon-wearables`
- edit `build.gradle` in root of your project and insert right at start global variables ( as described [here](https://github.com/asamm/locus-api/wiki/Adding-Locus-API-to-project#using-global-parameters) ):

	```
	// define global parameters
	ext {
	    compileSdkVersion = 25
	    buildToolsVersion = '25.0.2'

	    // define default parameters
	    minSdkVersion = 15
	    targetSdkVersion = 25

	    // signing of release version
	    signDebugPath = 'c:/menion/.../debug.keystore'
	    signDebugPassword = 'android'
	    signDebugKeyAlias = 'androiddebugkey'
	    signDebugKeyPassword = 'android'

	    signReleasePath = 'c:/menion/.../release.keystore'
	    signReleasePassword = 'password'
	    signReleaseKeyAlias = 'alias'
	    signReleaseKeyPassword = 'keyPassword'
	}
	```

- edit `settings.gradle` in root of your project and insert 
	```
	include ':addon-wearables:device'
	include ':addon-wearables:wear'
	```  
- finally refresh `gradle` and you are good to go

### Possible improvements

`TODO`

### Important information

Available at: not yet available
