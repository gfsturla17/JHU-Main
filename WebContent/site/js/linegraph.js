
/*These lines are all chart setup.  Pick and choose which chart features you want to utilize. */

window.x = function(score){ nv.addGraph(function() {
  var chart = nv.models.lineChart()
                .margin({left: 50})  //Adjust chart margins to give the x-axis some breathing room.
                .useInteractiveGuideline(false)  //We want nice looking tooltips and a guideline!
                .showLegend(false)       //Show the legend, allowing users to turn on/off line series.
                .showYAxis(true)        //Show the y-axis
                .showXAxis(true)        //Show the x-axis
                .forceY([0,1])
                .forceX([0,1])
  ;

  chart.xAxis     //Chart x-axis settings
  .axisLabel('Computation Time')
  .tickValues([0,1]);
  
  
  chart.yAxis     //Chart y-axis settings
  .axisLabel('Schedule Quality')
  .axisLabelDistance(.5)
  .tickValues([0,1])

  /* Done setting the chart up? Time to render it!*/
  
	  var myData = sinAndCos(score) 

  d3.select('#chart svg')    //Select the <svg> element you want to render the chart in.   
      .datum(myData)         //Populate the <svg> element with chart data...
      .call(chart);          //Finally, render the chart!
	  

  //Update the chart when window resizes.
  
  return chart;
});
}
/**************************************
 * Simple test data generator
 */
function sinAndCos(scores) {
  var score = scores;
  var sin = [];
  var points = score.split("|");
	
  for(i = 0; i <= points.length -1; i++){
		var coordinateVals = points[i].split(",");
		var xVal = coordinateVals[0];
		var yVal = 1 - coordinateVals[1];
		
	    sin.push({x: xVal, y: yVal});			
	}
  	

  //Line chart data should be sent as an array of series objects.
  return [
    {
      values: sin,      //values - represents the array of {x,y} data points
      key: '', //key  - the name of the series.
      color: '#0101DF'  //color - optional: choose your own line color.
    }
  ];
}

$(document).ready(function(){
	
});
