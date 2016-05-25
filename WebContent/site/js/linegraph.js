
/*These lines are all chart setup.  Pick and choose which chart features you want to utilize. */

window.x = function(score){ nv.addGraph(function() {
  var chart = nv.models.lineChart()
                .margin({left: 10})  //Adjust chart margins to give the x-axis some breathing room.
                .useInteractiveGuideline(true)  //We want nice looking tooltips and a guideline!
                .showLegend(true)       //Show the legend, allowing users to turn on/off line series.
                .showYAxis(true)        //Show the y-axis
                .showXAxis(true)        //Show the x-axis
  ;

  chart.xAxis     //Chart x-axis settings
      .axisLabel('Schedule Run #')
      .tickFormat(d3.format(',r'));

  chart.yAxis     //Chart y-axis settings
      .axisLabel('Score (v)')
      .tickFormat(d3.format('.02f'));

  /* Done setting the chart up? Time to render it!*/
  var myData = sinAndCos(score);   //You need data...

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
		var yVal = coordinateVals[1];
		
	    sin.push({x: xVal, y: yVal});			
	}
  	

  //Line chart data should be sent as an array of series objects.
  return [
    {
      values: sin,      //values - represents the array of {x,y} data points
      key: 'Sine Wave', //key  - the name of the series.
      color: '#ff7f0e'  //color - optional: choose your own line color.
    }
  ];
}

$(document).ready(function(){
	
});
