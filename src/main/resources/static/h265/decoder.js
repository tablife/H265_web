self.Module = {
    onRuntimeInitialized: function () {
        onWasmLoaded();
    }
};

var is_load = 0;
var is_wait_paly = 0;
var wait_play_url = "";
var ws_url = "";

function onWasmLoaded(){
	console.log("------------onWasmLoaded---------");
	is_load = 1;
	if(is_wait_paly == 1){
		start();
	}
}

importScripts("ffmpeg_decode.js");

self.onmessage = function (ev) {
	var data = ev.data;
	if(data.action == "play"){
		wait_play_url = data.url;
		ws_url = data.ws;
		if(is_load == 1){
			start();
		}else{
			is_wait_paly = 1;
		}
	}
}

function start(){
	var cacheBuffer = Module._malloc(200*1024);

	var videoCallback = Module.addFunction(function (buff, size, timestamp, width, height) {
		console.log("width="+width+"  height="+height);
		var outArray = Module.HEAPU8.subarray(buff, buff + size);
		var data = new Uint8Array(outArray);
		var yLength = width * height;
		var uvLength = (width / 2) * (height / 2);
		var data = {t:"frame", d:data, h:height, w:width};
		postMessage(data);
	});

	var ret = Module._createDecoder(videoCallback);
	console.log("_createDecoder ret="+ret);

	var websocket = new WebSocket(ws_url);
	websocket.binaryType = "arraybuffer";
	websocket.onopen = function(){
		console.log("ws onopen");
		var msg = {ver:"1.0" , url : wait_play_url};
		websocket.send(JSON.stringify(msg));
	};
	websocket.onmessage = function(msg){
		var typedArray = new Uint8Array(msg.data);
		Module.HEAPU8.set(typedArray, cacheBuffer);
		console.log("typedArray.length="+typedArray.length+"  canvasId=");
		var ret2 = Module._decode(ret, cacheBuffer, typedArray.length, 0);
		console.log("decode return " + ret2 + ".");
	};
	websocket.onerror = function(){
		console.log("ws onerror");
	};
	websocket.onclose = function(){
		console.log("ws onclose");
	};
}

