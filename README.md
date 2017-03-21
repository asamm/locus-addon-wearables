# Locus Map - add-on Wearables Mzperx's fork

### This addon is a fork of the original Locus Map - add-on Wearables. Many thanks to Asamm Software and Menion for providing it to open source!

The purpose of this fork to add functionalities what I would like to see when I use the watch for my workouts and for my outdoor activities.
I develop it for fun and when I have time.

My usage scenarios are: short-run (about 1 hour), long run (3-5 hours), hiking and cycling (< 8 hours). Usually I wear hr belt.

My expectations for short-run:
always-on screen, show hr, show statistical data like avg speed, distance etc...

My expectation for long-run and for other long lasting activities:
battery survives the activity,
see the battery level of the watch and the phone,
alert if I left the pre-selected route,
map where I can zoom in/out.

What functionalities what I have added so far:
- when track recoding is running there are 3 different screens. 
screen1: time elapsed, average speed, distance, cummulative altitude.
screen2: actual hr, avg hr, max hr.
screen3: control buttons (stop, pause, add waypoint) and the battery levels of the watch and the phone.
- Zooming function on the map screen: the screen are divided to three zones (top, middle, bottom) if you tap on a zone it will select a different zoom level (18,19,20).
- Ambient mode: when the watch is in ambient mode all data will be sinchronized less often to save battery. In ambient mode the map screen switches off (goes to black).

- Settings panel where you can customize the following settings:
  1. Ambient mode where you can control screen switching on/off and set long refresh period (once per minute). For the best battery saving set the all options to on.
  2. Alarms where you can set up low battery alarm for device or for watch. This is a visual alarm.
  3. Map screen: where you can switch on/off navigation panel and you can set up the zoom levels.

My test equipments:
- Nexus 5 (Android 6, Marshmallow)
- Samsung Gear Live (Android Wear 1.5), square screen (320x320)
- Polar H7
- On the pone:Locus Pro
- The addon is compiled with locus-api-android:0.2.7
