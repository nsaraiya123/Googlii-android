require("cloud/app.js");

Parse.Cloud.job("getLitzAccessKey", function(req, status)
{
    Parse.Cloud.httpRequest(
    {
        method: 'GET',
        url: 'https://api.litzscore.com/rest/v2/auth/?access_key=dc64370559960d3602a41dc8a2e51a56&secret_key=cfbf700d33633bebfdc00f046f57daee&app_id=com.googlii&device_id=abr344mkd99'
    }).then
    (
        function(httpResponse)
        {
            var params = JSON.parse(httpResponse.buffer);
            var litzAccessToken = params.auth.access_token;
            var LitzAccessKey = Parse.Object.extend("LitzAccessKey");
            var query = new Parse.Query(LitzAccessKey);
            query.get("8MBfz4WQZo",
            {
                success: function(litzAccessKey)
                {
                    litzAccessKey.set('accessKey', litzAccessToken);
                    status.success("LitzAccessKey Updated.");;
                    return litzAccessKey.save();
                },
                error: function(object, error)
                {
                    status.error("LitzAccessKey Parse Get Failed." + error);
                    return;
                }
            });
        },
        function(err)
        {
            console.log(err);
            status.error("LitzAccessKey Access Failed." + error);
        }
    );
});


Parse.Cloud.job("getLitzSchedule", function(req, status)
{
    // First Get LitzAccessKey
    var LitzAccessKey = Parse.Object.extend("LitzAccessKey");
    var query = new Parse.Query(LitzAccessKey);
    query.get("8MBfz4WQZo",
    {
        success: function(litzAccessKey)
        {
            var litzAccessToken = litzAccessKey.get("accessKey");
            var month = litzAccessKey.get("scheduleNextMonth");
            var scheduleUrl = 'https://api.litzscore.com/rest/v2/schedule/?access_token=' + litzAccessToken + '&date=' + month;
            Parse.Cloud.httpRequest(
            {
                method: 'GET',
                url: scheduleUrl
            }).then
            (
                function(httpResponse)
                {
                    var scheduleJson = JSON.parse(httpResponse.text);
                    var days = scheduleJson.data.months[0].days;
                    var scheduleArray = [];
                    for (var i=0; i<days.length; i++)
                    {
                        var matches = days[i].matches;
                        for (var j=0; j<matches.length; j++)
                        {
                            var match = matches[j];
                            if (match.format == 'test')
                                continue;
                            var name = match.name;
                            var Schedule = Parse.Object.extend("Schedule");
                            var schedule = new Schedule();
                            schedule.set("status", match.status);
                            schedule.set("relatedName", match.related_name);
                            schedule.set("name", match.name);
                            schedule.set("shortName", match.short_name);
                            schedule.set("title", match.title);
                            schedule.set("season", match.season.name);
                            schedule.set("format", match.format);
                            schedule.set("venue", match.venue);
                            schedule.set("teamA", match.teams.a.key);
                            schedule.set("teamB", match.teams.b.key);
                            schedule.set("litzID", match.key);
                            schedule.set("date", new Date(match.start_date.iso));
                            scheduleArray.push(schedule);
                        }
                    }
                    if (scheduleArray.length > 0)
                    {
                        Parse.Object.saveAll(scheduleArray,
                        {
                             success: function(objs)
                             {
                                litzAccessKey.set('scheduleNextMonth', scheduleJson.data.next_month);
                                status.success("Schedule Updated for " + month);
                                return litzAccessKey.save();
                             },
                             error: function(error)
                             {
                                console.log(error);
                                status.error("Parse Save Failed For Schedule Objects." + error);
                             }
                        });
                    }
                    else
                    {
                        console.log("No Scheduled Matches Found.");
                        status.error("No Scheduled Matches Found.");
                    }
                },
                function(error)
                {
                    console.log(error);
                    status.error("LitzScheduleApi Access Failed." + error);
                }
            );
        },
        error: function(object, error)
        {
            status.error("LitzAccessKey Parse Get Failed." + error);
            return;
        }
    });
});

Parse.Cloud.job("checkScheduleAndAddLiveScoreJob", function(req, status)
{
    var currentDate = new Date();
    var lagTime = (15 * 60 * 1000); // 15 min * 60 sec * 1000 millis
    var ltDate = new Date(currentDate.getTime() - (lagTime));
    var gtDate = new Date(currentDate.getTime() + (lagTime));
    var Schedule = Parse.Object.extend("Schedule");
    var query = new Parse.Query(Schedule);
    query.greaterThan('date', ltDate);
    query.lessThan('date', gtDate);

    query.find().then(function(results)
    {
        return results;
    }).then(function (results)
    {
        var liveMatchIDArray = [];
        for (var i = 0; i < results.length; i++)
        {
            var schedule = results[i];
            liveMatchIDArray.push(schedule.get("litzID"));
        }

        var LiveMatch = Parse.Object.extend("LiveMatch");
        var query2 = new Parse.Query(LiveMatch);
        query2.containedIn("litzID", liveMatchIDArray);
        query2.find(
        {
            success: function(results2)
            {
                var liveMatchArray = [];
                for (var j = 0; j < liveMatchIDArray.length; j++)
                {
                    var liveMatchID = liveMatchIDArray[j];
                    var bFound = false;
                    for (var k = 0; k < results2.length; k++)
                    {
                        var liveMatch = results2[k];
                        if (liveMatch.get("litzID") == liveMatchID)
                        {
                            bFound = true;
                            break;
                        }
                    }
                    if (bFound == false)
                    {
                        var newLiveMatch = new LiveMatch();
                        newLiveMatch.set("litzID", liveMatchID);
                        liveMatchArray.push(newLiveMatch);
                    }
                }
                if (liveMatchArray.length > 0)
                {
                    Parse.Object.saveAll(liveMatchArray,
                    {
                         success: function(objs)
                         {
                            status.success("Live Match Update Completed");
                         },
                         error: function(error)
                         {
                            console.log(error);
                            status.error("Parse Save Failed For Live Matches." + error);
                         }
                    });
                }
                else
                {
                    console.log("No Live Matches Found.");
                    status.success("No Live Matches Found.");
                }
            },
            error: function(error)
            {
                console.log(error);
                status.error("LiveMatch Search failed.");
            }
        });
    });
});

Parse.Cloud.job("updateLiveScoreJob", function(req, status)
{
    var LiveMatch = Parse.Object.extend("LiveMatch");
    var query = new Parse.Query(LiveMatch);
    query.find().then(function(results)
    {
        if (results.length == 0)
            status.success("No Live Matches Found.");
        return results;
    }).then(function (results)
    {
        for (var i = 0; i < results.length; i++)
        {
            var liveMatch = results[i];
            updateLiveScoreForMatch(liveMatch, status);
        }
    });
});

function updateLiveScoreForMatch(liveMatch, status)
{

    var litzMatchId = liveMatch.get("litzID");
    // First Get LitzAccessKey
    var LitzAccessKey = Parse.Object.extend("LitzAccessKey");
    var query = new Parse.Query(LitzAccessKey);
    query.get("8MBfz4WQZo",
    {
        success: function(litzAccessKey)
        {
            var litzAccessToken = litzAccessKey.get("accessKey");
            var matchUrl = 'https://api.litzscore.com/rest/v2/match/' + litzMatchId + '/?access_token=' + litzAccessToken;
            Parse.Cloud.httpRequest(
            {
                method: 'GET',
                url: matchUrl
            }).then (function(httpResponse)
            {
                var score = JSON.parse(httpResponse.text);
                var now = score.data.card.now;
                var teamPlayingNow = now.batting_team;
                var inning = 0;
                var teamPlayingFirst = score.data.card.batting_order[0];
                if (teamPlayingNow == teamPlayingFirst[0])
                    inning = 1;
                else
                    inning = 2;
                var teamPlayingNowName = score.data.card.teams[teamPlayingNow].short_name;

                var overs = score.data.card.now.recent_overs;
                var balls = score.data.card.balls;
                var ballIDArrayForSearch = [];
                for (var i=0; i<overs.length; i++)
                {
                    var over = overs[i];
                    var ballIDs = over[1];
                    for (var j=0; j<ballIDs.length; j++)
                        ballIDArrayForSearch.push(ballIDs[j]);
                }

                var ScoreDetails = Parse.Object.extend("ScoreDetails");
                var query2 = new Parse.Query(ScoreDetails);
                query2.containedIn("litzBallID", ballIDArrayForSearch);
                query2.find().then(function(results)
                {
                    var newBallArray = [];
                    for (var j = 0; j < ballIDArrayForSearch.length; j++)
                    {
                        var newBallID = ballIDArrayForSearch[j];
                        var bFound = false;
                        for (var k = 0; k < results.length; k++)
                        {
                            var savedBall = results[k];
                            if (savedBall.get("litzBallID") == newBallID)
                            {
                                bFound = true;
                                break;
                            }
                        }
                        if (bFound == false)
                        {
                            var scoreDetails = new ScoreDetails();
                            var newBallFromLitz = score.data.card.balls[newBallID];
                            var inning = 0;
                            var teamPlayingFirst = score.data.card.batting_order[0];
                            if (teamPlayingNow == teamPlayingFirst[0])
                              inning = 1;
                            else
                              inning = 2;
                            var teamPlayingNowName = score.data.card.teams[teamPlayingNow].short_name;

                            scoreDetails.set("litzBallID", newBallID);
                            scoreDetails.set("litzMatchId", litzMatchId);
                            scoreDetails.set("inning", inning);
                            scoreDetails.set("team", teamPlayingNowName);
                            scoreDetails.set("over", newBallFromLitz.over);
                            var runs = Number(newBallFromLitz.runs);
                            if (runs != "NaN")
                                scoreDetails.set("runs", newBallFromLitz.runs);
                            var wicket = Number(newBallFromLitz.wicket);
                            if (wicket != "NaN")
                                scoreDetails.set("wickets", wicket);
                            newBallArray.push(scoreDetails);
                        }
                    }
                    if (newBallArray.length > 0)
                    {
                        Parse.Object.saveAll(newBallArray,
                        {
                             success: function(objs)
                             {
                                status.success("Live Score Update Completed");
                             },
                             error: function(error)
                             {
                                console.log(error);
                                status.error("Parse Save Failed For Live Scores." + error);
                             }
                        });
                    }
                    else
                    {
                        status.success("Live Score Update Completed");
                    }

                    if (score.data.card.status == "completed")
                        liveMatch.destroy({});
                });
            });
        }
    });
}
