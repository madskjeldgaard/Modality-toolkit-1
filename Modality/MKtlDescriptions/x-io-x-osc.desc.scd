/*

////// basic example for cxosc in adhoc mode (default):

// 1. log into WIFI "x-OSC"

// 2. run this:
MKtlDesc.loadDescs;
x = MKtl(\x, "*x-osc"); x.gui;

// -> gui display should jitter slightly,
// -> and change clearly when you move the xosc!

/////// TROUBLESHOOTING:

/// 1. DOES MY XOSC REALLY SEND?
/// listen to all messages from \imu:
OSCdef(\imutest, { "oyoy ".scramble.postln }, \imu);
OSCdef(\imutest).disable;


// 2. in adhoc mode, xosc sends on its own wifi,
// from IP: NetAddr("169.254.1.1", 9000);
// to a broadcast IP: NetAddr("255.255.255.255", 8000);

//////// if you use custom settings, adjust like this:
x.device.destination.cs;
x.device.updateDstAddr(port: 9000);
x.device.updateSrcAddr(port: 9000);
x.device.updateRecvPort(8000);


******* NOT DONE YET: *******
--- see also long list on bottom ---

+ output communication

+ serial input messages are raw data, how to handle?

+ how to handle string only values?
  + ping returns strings,
  + sending back to set stuff uses strings
-> use PassSpec for not touching raw values



*/

(
deviceName: "x-osc",
protocol: \osc,
deviceType: \multiController,
elementTypes: [\accelerometer, \sensor, \serial],
status: (
	linux: "unknown after refactor, incomplete",
	osx: "imu, analog ins, digital ins tested and working.",
	win: "unknown after refactor, incomplete"
),

idInfo: "x-osc",
netAddrInfo: ( ipAddress: "169.254.1.1", srcPort: 9000, recvPort: 8000 ), // ip for adhoc mode

deviceInfo: (
	vendorURI: "http://www.x-io.co.uk/products/x-osc/",
	manualURI: "http://www.x-io.co.uk/downloads/x-OSC-User-Manual-v0.4.pdf",
	description: "WLan OSC I/O board",
	features: [
		"16× analogue/digital inputs",
		"16× digital/PWM outputs (up to 50 mA per channel)",
		"13-bit ADC with 400 Hz update rate per channel",
		"Up to 16-bit PWM resolution for 5 Hz to 250 kHz",
		"Control up to 400 RGB LEDs (NeoPixel)",
		"4× serial communication channels"
	],
	longName: "XIO x-OSC",
	notes: "",
),

specs: (
	\switch: [0, 1, \lin, 1],
	\pass: PassSpec,
	// measured degrees of rotation at 100 Hz sendrate:
	gyroAxis: [-1000, 1000],
	// measured max accel with fast shaking:
	accelAxis: [-8, 8],
	// guessing, usually more between +-0.5:
	magnetAxis: [-0.8, 0.8],
	// gyro temperature
	temp: [0, 100],
	// min and max working battery voltage
	batVolt: [3.4, 3.9]
),

collectives: (
	imu: (
		oscPath: '/imu',
		elements: [
			[\gyro, \pitch], [\gyro, \roll], [\gyro, \yaw],
			[\accel, \x], [\accel, \y], [\accel, \z],
			[\magnet, \pitch], [\magnet, \roll], [\magnet, \yaw],
			[\temp]
		],
		ioType: \in
	),
	dIn: (
		oscPath: '/inputs/digital',
		elements: 16.collect {|i|
			[\inD, (i + 1).asSymbol]
		},
		ioType: \in
	),
	aIn: (
		oscPath: '/inputs/analogue',
		elements: 16.collect {|i|
			[\inA, (i + 1).asSymbol]
		},
		ioType: \in
	),
	aCIn: (
		oscPath: '/inputs/analogue/comparator',
		elements: 16.collect {|i|
			[\inC, (i + 1).asSymbol]
		},
		ioType: \in
	)
),

elementsDesc: (
	// input elements:

	// 10 floating-point arguments indicating the current readings of the on board sensors. Arguments 1 – 3 gyroscope (°/s) x, y, z; arguments 4 – 6 accelerometer (g), x, y ,z; arguments 7 – 9 magnetometer (Ga) x, y ,z; argument 10 gyroscope temperature (°C)
	elements: [
		(
			key: \accel,
			shared: (elementType: 'accelAxis', ioType: \collectiveIn,
				\spec: 'accelAxis' ),
			elements: [\x, \y, \z].collect { |name, i|
				(key: name, style: (row: 0, column: i * 0.9 + 0, height: 2))
			}
		),
		(
			key: \gyro,
			shared: (elementType: 'gyroAxis', ioType: \collectiveIn,
				\spec: 'gyroAxis' ),
			elements: [\pitch, \roll, \yaw].collect{ |name, i|
				(key: name, style: (row: 0, column: i * 0.9 + 3, height: 2))
			}
		),
		(
			key: \magnet,
			shared: (elementType: 'gyroAxis', ioType: \collectiveIn,
				\spec: 'magnetAxis' ),
			elements: [\pitch, \roll, \yaw].collect { |name, i|
				(key: name, style: (row: 0, column: i* 0.9  + 6, height: 2))
			}
		),
		(
			key: \temp, elementType: 'cvIn', ioType: \collectiveIn,
			\spec: 'temp' ),

		// 1 float indicating the current battery voltage
		(
			key: \battery, oscPath: '/battery', ioType: \in,
			elementType: \voltage, spec: \batVolt),

		// 16 integer arguments represent the state of the 16 digital inputs (0 or 1). Sent only when the state of any digital input pin changes
		(
			key: \inD,
			shared: (elementType: \switch, spec: \switch, ioType: \collectiveIn),
			elements:  (1..16).collect { |num|  (key: num.asSymbol) }
		),
		(
			key: \led,
			shared: (elementType: \pad, spec: \unipolar, ioType: \out),
			elements:  (1..16).collect { |num|  (key: num.asSymbol) }
		),

		// 16 floating-point arguments indicate the analogue readings for all 16 inputs. Sent at the analogue input message rate
		(
			key: \inA,
			shared: (elementType: \slider, spec: \lin1, ioType: \collectiveIn),
			elements:  (1..16).collect { |num|  (key: num.asSymbol, style: (height: 2)) }
		),

		// 16 integer arguments represent the current compare state of the 16 analogue inputs (0 if below or 1 if above the threshold). Sent only when a threshold on any channel is crossed
		(
			key: \inC,
			shared: (elementType: \switch, spec: \switch, ioType: \collectiveIn),
			elements: (1..16).collect { |num|  (key: num.asSymbol,  height: 0.5) }
		),

		// // 1 blob argument containing the raw serial data received on input <channel> (1 – 4)
		// inS1: (oscPath: '/inputs/serial/1', inType: \blob, ioType: \in),
		// inS2: (oscPath: '/inputs/serial/2', inType: \blob, ioType: \in),
		// inS3: (oscPath: '/inputs/serial/3', inType: \blob, ioType: \in),
		// inS4: (oscPath: '/inputs/serial/4', inType: \blob, ioType: \in),

	]
)
);


///////////// NOTES - older list things to do look into

	// // /ping: 3 string arguments are the x-OSC local IP address (1st string), MAC address (2nd string) and firmware version (3rd string)
	// ping: (oscPath: \ping, numArgs: 3, ioType: \in),


	// output elements going to XOsc:
// If x-OSC receives a message that does not match any message in the following table the LED will flash red.

	// // send new IP for XOsc as string, e.g. “169.254.1.2”
	// setIP: (oscPath: "/osc/remote/ip", ioType: \out),
	// // send new remote port number for XOsc as int16 (0 – 65535)
	// setRemotePort: (oscPath: "/osc/remote/port", ioType: \out),
	// // send new local port number for XOsc as int16 (0 – 65535)
	// setLocalPort: (oscPath: "/osc/local/port", ioType: \out),
	// // int 1 enables using bundles, 0 disables
	// useBundles: (oscPath: "/osc/bundles", ioType: \out),
	// // int 1 enables using prefix (e.g. “/xosc”), 0 disables
	// usePrefix: (oscPath: "/osc/prefix/enabled", ioType: \out),
	// // set osc address prefix string, e.g. (e.g. “/xosc”)
	// prefix: (oscPath: "/osc/prefix/address", ioType: \out)


// /inputs/mode/analogue/<channel>
// sets the input <channel> (1 – 16) to analogue mode
// /inputs/mode/digital/<channel>
// sets the input <channel> (1 – 16) to digital mode
// /inputs/mode/serial/<channel>
// sets the input <channel> (1 – 16) to serial mode
// /inputs/analogue/read
// prompts x-OSC to respond with an /inputs/analogue
//
// /inputs/analogue/rate
// floating-point argument sets the message rate for the analogue input readings (0.0 – 400.0)
// /inputs/analogue/comparator/read
// prompts x-OSC to respond with an /inputs/analogue/compare message
// /inputs/analogue/comparator /threshold/<channel>
// floating-point argument sets the comparator threshold (0.0 – 1.0) for the specified input <channel> (1 – 16)
// /inputs/digital/read
// prompts x-OSC to respond with an /inputs/digital message
// /inputs/digital/up/<channel>
// enables the pull up resistor for the specified digital input <channel> (1 – 16)
// /inputs/digital/down/<channel>
// enables the pull down resistor for the specified digital input <channel> (1 – 16)
// /outputs/mode/digital/<channel>
// the specified output <channel> (1 – 16) is set to digital mode
// /outputs/mode/pulse/<channel>
// the specified output <channel> (1 – 16) is set to pulse mode
// /outputs/mode/pwm/<channel>
// the specified output <channel> (1 – 16) is set to PWM mode
// /outputs/mode/serial/<channel>
// the specified output <channel> (1 – 16) is set to serial mode
// /outputs/digital/<channel>
// integer argument (0 or 1) sets the state of the specified output <channel>
// /outputs/digital/pattern
// the lower 16-bits of the integer argument sets the state of all 16 digital outputs
// /outputs/pulse/<channel>
// triggers a pulse on the specified output <channel> (1 – 16) is set to pulse mode. Uses the last width and invert values
// /outputs/pulse/width/<channel>
// integer argument sets the pulse duration in milliseconds (0 – 60000) for the specified <channel>. Also triggers a pulse
// /outputs/pulse/invert/<channel>
// integer argument (0 or 1) sets the inversion state of the pulse (1 is inverted). Also triggers a pulse
// /outputs/pwm/frequency/<channel>
// floating-point argument sets the PWM frequency (5 – 250 000) for the specified output <channel> (1 – 16). Also sets the pin to PWM mode if not already
// /outputs/pwm/duty/<channel>
// floating-point argument sets the PWM duty cycle (0 – 100 %) for the specified output <channel> (1 – 16). Also sets the pin to PWM mode if not already
// /outputs/rgb/<channel>
// blob argument will set the RGB values of LEDs connected to <channel> (1 – 16). The blob should contain bytes in the format: R1, G1, B1, R2, G2, B2 ... Rn, Gn, Bn.
// /outputs/serial/<channel>
// blob argument will be transmitted on the specified serial
// ￼
// <channel> (1 – 4)
// /led/rgb
// three integer arguments (range 0 – 255) set the RGB vales for the on board LED
// /led/default
// resets the LED to the network mode colour
// /ping
// causes x-OSC to broadcast its local IP address
// /serial/baud/<channel>
// integer argument (0 – 2000000) sets the baud rate for the specified serial <channel> (1 – 4)
// /serial/buffer/<channel>
// integer argument (1 – 2048) sets the input buffer size for the specified serial <channel> (1 – 4)
// /serial/timeout/<channel>
// the timeout of the input buffer for the specified serial <channel> (1 – 4) is set to the value of i (0 – 1000 ms)
// /serial/framing/<channel>
// integer argument (-1 – 255) sets the framing character for the specified serial <channel> (1 – 4)

