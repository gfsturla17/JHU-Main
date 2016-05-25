$(document).ready(function(){
	
	$("#runSched").click(runSched);
	$("#emailSched").click(emailSchedule);

	$("#emailSched").hide();
	$("#userEmail").hide();
});


function cancelRun(){
	if (webSocket !== undefined) {
		//TODO make cancelRun actually cancel running the schedule
		webSocket.close();		
	}
}

var webSocket;
function runSched(){
	
    // Ensures only one connection is open at a time
    if(webSocket !== undefined && webSocket.readyState !== WebSocket.CLOSED){
       alert("Tests are already running!");
        return;
    }

    webSocket = new WebSocket("ws://localhost:8080/Hopkins_Web_App/actions");
     
    /**
     * Binds functions to the listeners for the websocket.
     */
    webSocket.onopen = function(event){
        if(event.data === undefined)
            return;
    };

    webSocket.onmessage = function(event){
        var data = event.data.split("~");
        alert["1"];
        var progress = data[1] + " ";
        console.log(progress);

        alert[progress];
        
        if(progress=="Error"){
        	cancelRun();
        	alert(data[0]);
        	
        }
        if(progress == "Score "){
        	var scores = data[0];
        	alert[data[0]];
        	window.x(data[0]);	
        }
        
        if(progress === "100"){
        	cancelRun();
        	alert("Finished Running Schedule");
    		$("#emailSched").show();
        	$("#userEmail").show();

        }      
    };

    webSocket.onclose = function(event){
    };
}

var emailSchedule = function() {
	var email = $("#userEmail").val();
	var jsonObj = {"userEmail": email};

	$.ajax({
			data: jsonObj,
			url: "EmailServlet",
			type: "post",
	}).done( function(res) {
		console.log(res);
		if(res == "Fail"){
			
		}else{
			alert("Email sent with attached schedule")
		}
	}).fail(function() {
		alert(res);
	});
};





