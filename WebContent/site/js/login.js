$(document).ready(function(){
	
	$("#submit").click(login);
	
});

var login = function() {
	var username = $("#username").val();
	var password = $("#password").val();

	var jsonObj = {"username": username, "password": password};

	$.ajax({
			data: jsonObj,
			url: "LoginServlet",
			type: "post",
	}).done( function(res) {
		console.log(res);
		if(res == "Fail"){
			alert("Incorrect Login Information Entered");
		}else{
			window.location="http://localhost:8080/Hopkins_Web_App/site/main.html";

		}
	}).fail(function(res) {
		alert(res);
	});
};