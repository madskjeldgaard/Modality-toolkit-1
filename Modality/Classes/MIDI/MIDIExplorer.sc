/* To Do:

1. abstract out consecutive note numbers and controller numbers
   in order to generate more compact code

2. check name consistency - is this already creating exactly the recommended names?

*/

MIDIExplorer {

	classvar <allMsgTypes = #[ \noteOn, \noteOff, \cc, \touch, \polytouch, \bend, \program ];

	classvar <resps;
	classvar <observeDict;
	classvar <>verbose = false;
	classvar <observedSrcID;
	classvar <totalSum = 0;

	*shutUp { verbose = false }

	// this could be in MKtlDesc
	*instructions {
		^"/**** desc file generated by MIDIExplorer.

Please adapt this file as follows:

1. Put any general info and comments about the device here.

2. Add some minimal code examples for testing, such as:

k = MKtl('nk2', \"korg-nanokontrol2\");
k.elementAt(\sl, 0).action = { \yo.postln; };
k.elementAt(\sl, 1).action = { 1.postcs; };

3. When there are only a few elements, one can keep them in a flat dictionary, and give them clear element names; see
'Reference/Naming_conventions_in_element_descriptions'.openHelpFile

4. When many elements of the same type are listed in an obvious order,
   such as 8 sliders sending cc numbers,
   or 88 piano keys sending noteOn and noteOff,
   it makes sense to organize them into Arrays.
'Reference/MKtl_description_files'.openHelpFile

Instructions will be continued when canonical format is been finalized.
More information can be found here:
'Tutorials/How_to_create_a_description_file'.openHelpFile;
'Tutorials/How_to_create_a_description_file_for_MIDI'.openHelpFile;


****/\n\n"

	}

	// also move to MKtlDesc later
	*compile {
		var descString =
"(
deviceName: %,
protocol: %,
deviceType: '___',
elementTypes: %,
status: (
	linux: %,
	osx: %,
	win: %),

idInfo: %,

// hardwarePages: [1, 2, 3, 4],

// deviceInfo: (
// vendorURI: 'http://company.com/products/this',
// manualURI: 'http://company.com/products/this/manual.pdf',
	// description: ,
	// features: [],
	// notes: ,
	// hasScribble: false
// ),
elementsDesc: %
);\n\n".format(
			this.deviceName.cs,
			'midi'.cs,
			[],  // elementTypes
			"unknown".cs, // status
			"unknown".cs,
			"unknown".cs,
			this.deviceName.cs, // same as idInfo
			this.compileDesc
		);

		^this.instructions ++ descString
	}


	*init {

		resps = [

			MIDIFunc.cc({|val, num, chan, src|
				if (src == observedSrcID) {
					this.updateRange(\cc, val, num, chan, src);
				};
			}),

			MIDIFunc.noteOn({|val, num, chan, src|
				if (src == observedSrcID) {
					this.updateRange(\noteOn, val, num, chan, src);
				}
			}),

			MIDIFunc.noteOff({|val, num, chan, src|
				if (src == observedSrcID) {
					this.updateRange(\noteOff, val, num, chan, src);
				}
			}),

			MIDIFunc.polytouch({|val, note, chan, src|
				if (src == observedSrcID) {
					this.updateRange(\polytouch, val, note, chan, src);
				};
			}),

			MIDIFunc.bend({|val, chan, src|
				if (src == observedSrcID) {
					this.updateRange(\bend, val, 0, chan, src);
				};
			}),

			MIDIFunc.touch({|val, chan, src|
				if (src == observedSrcID) {
					this.updateRange(\touch, val, 0, chan, src);
				}
			}),

			MIDIFunc.program({|val, chan, src|
				if (src == observedSrcID) {
					this.updateRange(\program, val, 0, chan, src);
				}
			})
		];
	}

	*start { |srcID|
		if (resps.isNil) { this.init };

		observedSrcID = srcID;
		this.prepareObserve;
		resps.do(_.add);
	}

	*stop { |srcID|
		if ( srcID.notNil ){
			observedSrcID = nil;
		};
		resps.do(_.free);
 	}

	*postSrcInfo {
		MIDIClient.sources.do { |src, i|
			"i: % - idInfo: % - name: % - uid: % \n".postf(
				i, src.device, src.name, src.uid
			);
		};
	}

	*prepareObserve {
		totalSum = 0;
		observeDict = ();
		allMsgTypes.do(observeDict.put(_, Dictionary()));
	}

	*openDoc { |name|
		name = name ? "edit and save me";
		// works only on 3.7:
		Document( name ++ ".desc.scd", this.compile );
	}

	*deviceName {
		var string, device;
		device = MIDIClient.sources.detect { |src| src.uid === observedSrcID };
		if (device.notNil) { ^device.device };
		string = "(No source with uid % present.\n Pick one from the posted list:)";
		string.postf(observedSrcID);
		this.postSrcInfo;
		^string
	}

	*compileDesc { |includeSpecs = false|

		var num, chan;

		var str = "( \n\t elements: ["; // ])

		if (observeDict[\cc].notEmpty) {
			str = str + "\n\n\t\t// ------ cc -------------";
			observeDict[\cc].sortedKeysValuesDo { |key, val|
				#chan, num = key.split($_).collect(_.asInteger);
				str = str + "\n\t\t( key: 'cc_%', 'midiNum':  %, 'midiChan': %, 'midiMsgType': 'cc', 'elementType': 'slider', 'spec': 'midiCC'),"
					.format(key, num, chan);
			};
		};

		if (observeDict[\noteOn].notEmpty) {
			str = str + "\n\n\t\t// ------ noteOn -------------";
			observeDict[\noteOn].sortedKeysValuesDo { |key, val|
				#chan, num = key.split($_).collect(_.asInteger);
				str = str + "\n\t\t( key: 'nOn_%', 'midiNum':  %, 'midiChan': %, 'midiMsgType': 'noteOn', 'elementType': 'pad', 'spec': 'midiVel'),"
					.format(key, num, chan);
			};
		};

		if (observeDict[\noteOff].notEmpty) {
			str = str + "\n\n\t\t// ------ noteOn -------------";
			observeDict[\noteOff].sortedKeysValuesDo { |key, val|
				#chan, num = key.split($_).collect(_.asInteger);
				str = str + "\n\t\t( key: 'nOff_%', 'midiNum':  %, 'midiChan': %, 'midiMsgType': 'noteOff', 'elementType': 'pad', 'spec': 'midiVel'),"
					.format(key, num, chan);
			};
		};

		// if (observeDict[\polytouch].notEmpty) {
		// 	str = str + "\n\n// ------ polytouch -------------";
		// 	observeDict[\polytouch].sortedKeysValuesDo { |key, val|
		// 		#chan, num = key.split($_).collect(_.asInteger);
		// 		str = str + "\n'_polytouch_%_': ('midiMsgType': 'polyTouch', 'type': 'keytouch', 'midiChan': %, 'midiNum':  %,'spec': 'midiCC'),"
		// 		.format(key, chan, num);
		// 	};
		// };
		//
		// if (observeDict[\touch].notEmpty) {
		// 	str = str + "\n\n// ------- touch ------------";
		// 	observeDict[\touch].sortedKeysValuesDo { |key, val|
		// 		#chan, num = key.split($_).collect(_.asInteger);
		// 		str = str + "\n'_touch_%_': ('midiMsgType': 'touch', 'type': 'chantouch', 'midiChan': %, 'midiNum':  %,'spec': 'midiTouch'),".format(key, chan, num);
		// 	};
		// };
		//
		// if (observeDict[\bend].notEmpty) {
		// 	str = str + "\n\n// ------- bend ------------";
		// 	observeDict[\bend].sortedKeysValuesDo { |key, val|
		// 		#chan, num = key.split($_).collect(_.asInteger);
		// 		str = str + "\n'_bend_%_': ('midiMsgType': 'bend', 'type': 'bender', 'midiChan': %, 'midiNum':  %,'spec': 'midiBend'),".format(key, chan, num);
		// 	};
		// };

		str = str + "\n\t]\n)\n";

		// if (includeSpecs) {
		// 	str = str + "\n\n// ----- noteOn Specs ----------";
		// 	observeDict[\noteOn].sortedKeysValuesDo { |key, val|
		// 		str = str + "\nMKtl.addSpec( _elName_%_, [%, %, \linear, 0, 0]);".format(key, val[0], val[1]);
		// 	};
		// 	str = str + "\n\n// ----- noteOff Specs ----------";
		// 	observeDict[\noteOn].sortedKeysValuesDo { |key, val|
		// 		str = str + "\nMKtl.addSpec( _elName_%_, [%, %, \linear, 0, 0]);".format(key, val[0], val[1]);
		// 	};
		// 	str = str + "\n\n// ----- CC Specs ----------";
		// 	observeDict[\noteOn].sortedKeysValuesDo { |key, val|
		// 		str = str + "\nMKtl.addSpec( _elName_%_, [%, %, \linear, 0, 0]);".format(key, val[0], val[1]);
		// 	};
		// 	str = str + "\n\n// ----- bend Specs ----------";
		// 	observeDict[\noteOn].sortedKeysValuesDo { |key, val|
		// 		str = str + "\nMKtl.addSpec( _elName_%_, [%, %, \linear, 0, 0]);".format(key, val[0], val[1]);
		// 	};
		// };

		^str;
	}

	*postObs { |post = false|
		var total = observeDict.collect(_.size).sum;
		if (total > totalSum or: post) {
			"\nMIDIExplorer \n- total number of controls so far: %\n".postf(total);
			allMsgTypes.do { |type|
				var dict = observeDict[type], size;
				if (dict.notNil and: { size = dict.size; size > 0 }) {
					"  %: %".postf(type, size);
				};
			};
			totalSum = total;
			"".postln;
		};
	}

	*updateRange { |msgType, val, num, chan|
		var hash, range;
		var msgDict = observeDict[msgType];

		if (verbose) { [msgType, val, num, chan].postcs; } { ".".post; };
		this.postObs;

		hash = "%_%".format(chan, (num + 1000).asString.drop(1));
		range = msgDict[hash];
		range.isNil.if{
			// min max
			msgDict[hash] = range = [val, val];
		};

		msgDict[hash] = [min(range[0], val), max(range[1], val)];
	}
}
