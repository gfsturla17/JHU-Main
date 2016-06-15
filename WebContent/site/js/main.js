$(document).ready(function(){
	
	$("#runSched").click(runSched);
	$("#runSched").click(runSched);
	$("#emailSched").click(emailSchedule);
	$("#uploadButton").show();
	$("#scheduleScore").show();
	
	document.getElementById('emailSched').disabled = true;
	document.getElementById('userEmail').disabled = true;
	$("#emailSched").show();
	$("#userEmail").show()
	$("#spinner").hide();
	window.x("0,0|");	

	$('input[type="file"]').ajaxfileupload({
	      'action': 'FileUploader',	      	    
	  'onComplete': function(response) {	        
	        $('#upload').hide();
	        alert("File has been uploaded");
	      },
	      'onStart': function() {
	        $('#upload').show(); 
	      }
	 });
	
});


function cancelRun(){
	if (webSocket !== undefined) {
		//TODO make cancelRun actually cancel running the schedule
		webSocket.close();		
	}
}

var webSocket;
function runSched(){
	$("#spinner").show();
	
    // Ensures only one connection is open at a time
    if(webSocket !== undefined && webSocket.readyState !== WebSocket.CLOSED){
        return;
    }

    if (document.getElementById('quick').checked) {
    	webSocket = new WebSocket("ws://ec2-54-82-164-57.compute-1.amazonaws.com:8080/jhu/actions");
//    	webSocket = new WebSocket("ws://ec2-184-72-70-197.compute-1.amazonaws.com:8080/hopkins/actions");
    	
    	
    }else if(document.getElementById('thorough').checked){
    	webSocket = new WebSocket("ws://ec2-54-82-164-57.compute-1.amazonaws.com:8080/jhu/thorough");
//    	webSocket = new WebSocket("ws://ec2-184-72-70-197.compute-1.amazonaws.com:8080/hopkins/thorough");

    }
    
     
    /**
     * Binds functions to the listeners for the websocket.
     */
    webSocket.onopen = function(event){
        if(event.data === undefined)
            return;
    };

    webSocket.onmessage = function(event){
        var data = event.data.split("~");
        var progress = data[1] + " ";
        var update = data[0] + " ";
        console.log(progress);

        
        if(progress=="Error"){
        	cancelRun();
        	alert(data[0]);
        	
        }
        if(progress == "Score "){
        	var scores = data[0];
        	alert[data[0]];
        	window.x(data[0]);	
        }
        
        if(progress == "ThoroughUpdate "){
            console.log(update);

        	document.getElementById("scheduleScore").innerHTML = "New best schedule found on:  " + update;
        }
        if(progress == "Parsing "){
        	$("#spinner").hide();
        }
        
        if(progress == "100 "){
        	alert("Finished Running Schedule");
        	document.getElementById('emailSched').disabled = false;
        	document.getElementById('userEmail').disabled = false;
    		$("#emailSched").show();
        	$("#userEmail").show();
        	$("#runSched").click(runSched);
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

var uploadFile = function(){
	alert("upload");
	 $("#file").ajaxfileupload({
	      'action': 'FileUploader',	      	    
	  'onComplete': function(response) {	        
	        alert("File SAVED!!");
	      },
	      'onStart': function() {
	        $('#upload').show(); 
	      }
	 });

}   







