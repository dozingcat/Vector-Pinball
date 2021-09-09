# Vector Pinball

Vector Pinball is a pinball game for Android devices.
It is released under version 3 of the GPL; see [COPYING](COPYING.txt) for the license text.

The graphics are deliberately simple; currently everything is drawn with lines and circles.
The focus is on gameplay and accurate physics.
It uses the libgdx Java wrapper for the Box2D physics engine.
Thanks to Peter Drescher for the sound effects; see [his article on creating them](https://www.twittering.com/webarchive_articles/FMOD%20for%20Android%20-%20O'Reilly%20Broadcast.html).

The GitHub project page is: [github.com/dozingcat/Vector-Pinball/](https://github.com/dozingcat/Vector-Pinball/).
See [devnotes.txt](devnotes.txt) for an overview of the code layout.

There is a very experimental table editor at [github.com/dozingcat/Vector-Pinball-Editor/](https://github.com/dozingcat/Vector-Pinball-Editor/)

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

Vector Pinball includes [the libgdx library by Bad Logic Games](http://libgdx.badlogicgames.com/).
libgdx is used under [the terms of Version 2.0 of the Apache License](https://www.apache.org/licenses/LICENSE-2.0).

Sound, music, & audio code by [pdx of Twittering Machine](http://www.twittering.com).

# How to Translate

Make a new folder in "app/src/main/res" named "values-**_lang_**" (with only at least 2 letters), copy-paste the strings.xml file from the "values" folder into it, and start editing it to the language of your choice.

Also make a folder in "fastlane/metadata/android" with a folder named "**_lang_**" (with only at least 2 letters), and copy the .txt files from "en_US", and simply overwrite them with the description in your own language. The "title.txt" file is entirely optional if you feel like the title could also be translated.

## Download

[![Download from F-Droid](https://camo.githubusercontent.com/7df0eafa4433fa4919a56f87c3d99cf81b68d01c/68747470733a2f2f662d64726f69642e6f72672f77696b692f696d616765732f632f63342f462d44726f69642d627574746f6e5f617661696c61626c652d6f6e2e706e67 "Download from F-Droid")](https://f-droid.org/repository/browse/?fdid=com.dozingcatsoftware.bouncy)    [![Download from Google Play](https://user-images.githubusercontent.com/33793273/132640445-ee1c74c2-9330-4ba9-93f8-218acd52fab9.png "Download from Google Play")](https://play.google.com/store/apps/details?id=com.dozingcatsoftware.bouncy)
