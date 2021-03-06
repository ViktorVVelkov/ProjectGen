package com.mycompany.app.algorithms;


import com.mycompany.app.inserts.data.Inserter_LecturesAssigned;
import com.mycompany.app.inserts.data.TwoInts;
import com.mycompany.app.timetabling.*;

import java.sql.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

public class GreedyAlgorithm {
    private static String s_mostRecentDate = "";                //maybe a good idea to set it to always be a Monday, create a function to always align this parameter to a Monday
    private static String s_lastmostRecentDate = "";
    private Connection connection;
    private ArrayList<Duplet> initial = new ArrayList<>();
    private ArrayList<Duplet> assigned = new ArrayList<>();
    private ArrayList<TwoInts> twoInts;

    public ArrayList<TwoInts> getTwoInts() {
        return twoInts;
    }

    public void setTwoInts(ArrayList<TwoInts> twoInts) {
        this.twoInts = twoInts;
    }

    public GreedyAlgorithm(Connection connection){
        this.connection = connection;
    }

    /**/
    private Week_Timetable timetable;
    // Taken from https://howtodoinjava.com/java/date-time/dates-between-two-dates/
    public static List<Date> getDaysBetweenDates(Date startdate, Date enddate) {
        List<Date> dates = new ArrayList<Date>();
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(startdate);

        while (calendar.getTime().before(enddate))
        {
            Date result = calendar.getTime();
            int a = result.getDay();                                //delete this after test
            if(result.getDay() != 6) {
                if (result.getDay() != 0) {
                    dates.add(result);
                }
            }
            calendar.add(Calendar.DATE, 1);

        }
        return dates;
    }
    // heuristics for the students' preferences
    private PreferredDays o_getPreferencesStudents(String sCourse, int iOnPrivilege, int iMultiplicator) throws SQLException {

        PreferredDays prefdays = new PreferredDays();

        ArrayList<String> mylist = new ArrayList<>();                                   //days M, T, W ...
        ArrayList<String> myResult = new ArrayList<>();                                 // chosen day(s)

        List<Integer> studentsChoice = new ArrayList<>();                               // for each day a number of people voted
        List<Double> studentsChoiceHeuristics = new ArrayList<>();                      // for each day calculate heuristics, based on student's preference
        List<Integer> preferredHour = new ArrayList<>();                                // preferred hour of day
        List<Integer> preferredHour_2 = new ArrayList<>();                              // preferred hour of day .2
        List<Integer> preferredHourHeuristics = new ArrayList<>();                      // for each Hour count people voted
        List<Integer> preferredHour_2Heuristics = new ArrayList<>();                    // for each Hour count people voted

        ArrayList<Double> weightOnChoice = new ArrayList<>();
        String sSS = "";
        String sql =
                "SELECT c.prefday, c.scale, c.privileged, c.preftime, c.preftime_secondchoice FROM( " +
                        "SELECT * FROM curiculum.main_preferences_lectures_students  a " +
                        "INNER JOIN preferences b " +
                        "ON a.kings_id_fk = b.kings_id )as c " +
                        "WHERE c.course_abreviation = ?";  //

        mylist.add("Monday");
        mylist.add("Tuesday");
        mylist.add("Wednesday");
        mylist.add("Thursday");
        mylist.add("Friday");

        studentsChoice.add(0);
        studentsChoice.add(0);
        studentsChoice.add(0);
        studentsChoice.add(0);
        studentsChoice.add(0);

        studentsChoiceHeuristics.add(0.0);
        studentsChoiceHeuristics.add(0.0);
        studentsChoiceHeuristics.add(0.0);
        studentsChoiceHeuristics.add(0.0);
        studentsChoiceHeuristics.add(0.0);

        for(int i= 0; i < 11; i++){
            preferredHour.add(900 + i*100);
            preferredHour.add(900 + i*100 + 30);
            preferredHour_2.add(900 + i*100);
            preferredHour_2.add(900 + i*100 + 30);
            preferredHourHeuristics.add(0);
            preferredHourHeuristics.add(0);
            preferredHour_2Heuristics.add(0);
            preferredHour_2Heuristics.add(0);
        }

        PreparedStatement prt = connection.prepareStatement(sql);
        prt.setString(1, sCourse);
        ResultSet rst = prt.executeQuery();

        while(rst.next()) {
            sSS = rst.getString("prefday");
            sSS = sSS.toLowerCase(Locale.ROOT);

            int iHour = rst.getInt("c.preftime");
            int iHour2 = rst.getInt("c.preftime_secondchoice");

            for(int i = 0; i < preferredHour.size(); i++) {
                if (iHour == preferredHour.get(i)) {
                    preferredHourHeuristics.set(i, preferredHourHeuristics.get(i) + 1);
                    break;
                }
            }
            for(int i = 0; i < preferredHour_2.size(); i++){
                if(iHour2 == preferredHour_2.get(i)){
                    preferredHour_2Heuristics.set(i, preferredHour_2Heuristics.get(i) + 1 );
                    break;
                }
            }

            if (sSS.equals("monday")) {
                studentsChoice.set(0, studentsChoice.get(0) + 1);
                if (iOnPrivilege == 1)      // this means if I have chosen to count privileged students as better heuristics
                    studentsChoiceHeuristics.set(0, studentsChoiceHeuristics.get(0) + rst.getInt("c.scale") * 0.9 + rst.getInt("c.privileged") * iMultiplicator);
                else studentsChoiceHeuristics.set(0, studentsChoiceHeuristics.get(0) + rst.getInt("c.scale"));            }
            ;

            if (sSS.equals("tuesday")) {
                studentsChoice.set(1, studentsChoice.get(1) + 1);
                if (iOnPrivilege == 1)
                    studentsChoiceHeuristics.set(1, studentsChoiceHeuristics.get(1) + rst.getInt("c.scale") * 0.9 + rst.getInt("c.privileged") * iMultiplicator);
                else studentsChoiceHeuristics.set(1, studentsChoiceHeuristics.get(1) + rst.getInt("c.scale"));            }
            ;

            if (sSS.equals("wednesday")) {
                studentsChoice.set(2, studentsChoice.get(2) + 1);
                if (iOnPrivilege == 1)
                    studentsChoiceHeuristics.set(2, studentsChoiceHeuristics.get(2) + rst.getInt("c.scale") * 0.9 + rst.getInt("c.privileged") * iMultiplicator);
                else studentsChoiceHeuristics.set(2, studentsChoiceHeuristics.get(2) + rst.getInt("c.scale"));            }
            ;

            if (sSS.equals("thursday")) {
                studentsChoice.set(3, studentsChoice.get(3) + 1);
                if (iOnPrivilege == 1)
                    studentsChoiceHeuristics.set(3, studentsChoiceHeuristics.get(3) + rst.getInt("c.scale") * 0.9 + rst.getInt("c.privileged") * iMultiplicator);
                else studentsChoiceHeuristics.set(3, studentsChoiceHeuristics.get(3) + rst.getInt("c.scale"));            }
            ;

            if (sSS.equals("friday")) {
                studentsChoice.set(4, studentsChoice.get(4) + 1);
                if (iOnPrivilege == 1)
                    studentsChoiceHeuristics.set(4, studentsChoiceHeuristics.get(4) + rst.getInt("c.scale") * 0.9 + rst.getInt("c.privileged") * iMultiplicator);
                else studentsChoiceHeuristics.set(4, studentsChoiceHeuristics.get(4) + rst.getInt("c.scale"));

            }
        }
        ///

        prt.close();
        rst.close();


        int   highestIndex = -1;
        double     highest = 0.0;
        int highestIndex2 = -1;
        int highestIndex3 = -1;
        int bestHour = 0;
        int bestHour_2 = 0;
        for(int i = 0; i < studentsChoiceHeuristics.size(); i++){
            if(  highest < studentsChoiceHeuristics.get(i)){
                highest = studentsChoiceHeuristics.get(i);
                highestIndex = i;
            }
        }

        myResult.add(mylist.get(highestIndex));
        weightOnChoice.add(studentsChoiceHeuristics.get(highestIndex));
        for(int i = 0; i < studentsChoice.size(); i++){

            if(  highest == studentsChoiceHeuristics.get(i) && i != highestIndex ){
                myResult.add(mylist.get(i));
                weightOnChoice.add(studentsChoiceHeuristics.get(i));
            }
        }

        for(int i = 0; i < preferredHourHeuristics.size(); i++){
            if(  bestHour < preferredHourHeuristics.get(i)){
                bestHour = preferredHourHeuristics.get(i);
                highestIndex2 = i;
            }
        }
        for(int i = 0; i < preferredHour_2Heuristics.size(); i++){
            if(  bestHour_2 < preferredHour_2Heuristics.get(i)) {
                bestHour_2 = preferredHour_2Heuristics.get(i);
                highestIndex3 = i;
            }
        }


        prefdays.setPrefDay(myResult);
        //empty for peopleVoted
        countPeopleVotedForPrefDays(prefdays, myResult, sCourse);
        prefdays.setDayHeuristics(weightOnChoice);
        prefdays.setiPrefHour(preferredHour.get(highestIndex2));
        prefdays.setiPrefHour_2(preferredHour_2.get(highestIndex3));
        return prefdays;
    }
    
    public void countPeopleVotedForPrefDays(PreferredDays prefs, ArrayList<String> days, String sCourse) throws SQLException {
        String sql = "SELECT COUNT(*) FROM main_preferences_lectures_students WHERE course_abreviation = ? AND LOWER(prefday) = ?";
        String sDay;
        ArrayList<Integer> people = new ArrayList<>();
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1, sCourse);
        for(int i = 0; i < days.size(); i++){
            sDay = days.get(i).toLowerCase(Locale.ROOT);
            preparedStatement.setString(2,sDay);
            ResultSet rst = preparedStatement.executeQuery();
            while(rst.next()){
                int numberOfPeople = rst.getInt(1);
                people.add(numberOfPeople);
            }
            rst.close();
        }
        preparedStatement.close();
        prefs.setpeopleVoted(people);

    }
    
    
    
    // heuristics for the lecturer's preferences
    private String s_getLecturersChoice(String sCourse) throws SQLException {
        String sql = "SELECT prefday FROM curiculum.main_preferences_lectures_teachers WHERE course_abreviation = ? ";
        String sSS = "";

        PreparedStatement prt = connection.prepareStatement(sql);
        prt.setString(1, sCourse);
        ResultSet rst = prt.executeQuery();
        while (rst.next()){
            sSS = rst.getString(1);
        }
        return sSS;
    }
    // Lectures
    private ArrayList<Duplet> lecturesToBeAssigned2(ArrayList<TwoInts> codesOfLectures, int iSemester, String sTable) throws SQLException {
        ArrayList<Duplet> initial2 = new ArrayList<>();
        String sql77 = "SELECT abreviation, hours_twoweeks "+
                "FROM " + sTable + " " +
                "WHERE hours_twoweeks != 0 " +
                "AND inside_code = ?" ;

        PreparedStatement preparedStatement = connection.prepareStatement(sql77);
        Iterator iter = codesOfLectures.iterator();

        while(iter.hasNext()){
            TwoInts twoInts = (TwoInts) iter.next();
            preparedStatement.setInt(1, twoInts.getiCode());
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()){
                double iHours = resultSet.getInt("hours_twoweeks")/2; // remember that some lectures may have 5 hours per two weeks split 2 + 3
                double iCheck = iHours/0.5;
                if(iCheck*(int)iHours != iHours*iCheck){
                    initial2.add(new Duplet(resultSet.getString(1),iHours - 0.5,twoInts.getiAttending(), twoInts.getiCode()));
                    initial2.add(new Duplet(resultSet.getString(1),iHours + 0.5,twoInts.getiAttending(), twoInts.getiCode()));
                }
                else{
                    initial2.add(new Duplet(resultSet.getString(1),iHours,twoInts.getiAttending(), twoInts.getiCode()));
                    initial2.add(new Duplet(resultSet.getString(1),iHours,twoInts.getiAttending(), twoInts.getiCode()));
                }
            }
            resultSet.close();
        }

        preparedStatement.close();


        return initial2;
    }

    private ArrayList<Duplet> lecturesToBeAssigned(int iSemester, String sTable , int iYear) throws SQLException {

        String sDescription = "SEM" + Integer.toString(iSemester);
        String sql22 = "SELECT abreviation,hours_twoweeks, inside_code " +
                        "FROM  " + sTable + " " +
                        "WHERE hours_twoweeks != 0 " +
                        "AND DESCRIPTION LIKE ? " ; //+
                        //"AND YEAR = " + Integer.toString(iYear);
        int iFinalNumber = 0;
        int iCode = 0;
        int iNumberofStudentsAttending = countStudents(1);
        int iNumberofStudentsAttending2 = countStudents(1);
        int iNumberofStudentsAttending3 = countStudents(1);

        PreparedStatement statement = connection.prepareStatement(sql22);
        statement.setString(1, sDescription + "%");
        ResultSet resSet = statement.executeQuery();

        while(resSet.next()){
            iCode = resSet.getInt(3);
            String abrev = resSet.getString("abreviation");
            if(abrev.startsWith("4CCS")){iFinalNumber = iNumberofStudentsAttending;}
            else if(abrev.startsWith("5CCS")){iFinalNumber = iNumberofStudentsAttending2;}
            else if(abrev.startsWith("6CCS")){iFinalNumber = iNumberofStudentsAttending3;}
            else{}
            double iHours = resSet.getInt("hours_twoweeks")/2; // remember that some lectures may have 5 hours per two weeks split 2 + 3
            double iCheck = iHours/0.5;
            if(iCheck*(int)iHours != iHours*iCheck){
                                initial.add(new Duplet(abrev,iHours - 0.5,iFinalNumber, iCode));
                                initial.add(new Duplet(abrev,iHours + 0.5,iFinalNumber, iCode));
             }
            else{
                                initial.add(new Duplet(abrev,iHours,iFinalNumber, iCode));
                                initial.add(new Duplet(abrev,iHours,iFinalNumber, iCode));
            }

            //System.out.println(initial.get(index));
            //index++;
            //index++;
        }

        statement.close();
        resSet.close();

        return initial;

    }
    //please check comments on line
    public Week_Timetable myTimetable(int iNumDays_TellMeHowManyDaysYouWant) throws SQLException, ParseException {

        List<Date> datesList = new ArrayList<>();
        String s_Temp = "";

        if (s_mostRecentDate.isEmpty()) {
            //initial search for date
            String sql64 =
                            "(SELECT MIN(DATE), MIN(MONTH), MIN(YEAR) " +
                            "FROM two_weeks_availability_halls_bush_house " +
                            "WHERE MONTH = (SELECT MIN(MONTH) FROM two_weeks_availability_halls_bush_house) " +
                            "UNION " +
                            "SELECT MIN(DATE), MIN(MONTH), MIN(YEAR) " +
                            "FROM two_weeks_availability_halls_waterloo " +
                            "WHERE MONTH = " +
                            "(SELECT MIN(MONTH) FROM two_weeks_availability_halls_waterloo) )  " + " LIMIT 1";                                       //maybe use a union here in case on of the tables is empty
            Statement statement = connection.createStatement();
            ResultSet rst = statement.executeQuery(sql64);
            while (rst.next()) {
                s_mostRecentDate = Integer.toString(rst.getInt(1)) + "/" + Integer.toString(1 + rst.getInt(2)) + "/" + Integer.toString(rst.getInt(3));
            }
            rst.close();
            statement.close();
        }

            {/* update the mostrecentday to be one n days ahead   */
                    Date date1 = new SimpleDateFormat("dd/MM/yyyy").parse(s_mostRecentDate);
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(date1);
                    calendar.add(Calendar.DAY_OF_MONTH, iNumDays_TellMeHowManyDaysYouWant);
                    Date date2;
                    date2 = calendar.getTime();
                    s_Temp = Integer.toString(date2.getDate()) + "/" + Integer.toString(date2.getMonth() + 1) + "/" + Integer.toString(date2.getYear()+1900);

            /* end of update   */
            }


            timetable = new Week_Timetable();                                                                   //assign a new Week_Timetable object
            timetable.setiNumDays(iNumDays_TellMeHowManyDaysYouWant);                                           //set parameters
            timetable.setsStartDay(s_mostRecentDate);
            datesList = getDaysBetweenDates(new SimpleDateFormat("dd/MM/yyyy").parse(s_mostRecentDate), new SimpleDateFormat("dd/MM/yyyy").parse(s_Temp));
            s_lastmostRecentDate = s_mostRecentDate;
            s_mostRecentDate = s_Temp;                                                                          //most recent date updated
            timetable.setsEndDay(s_mostRecentDate);

            for(int i = 0; i < datesList.size(); i++){
                String sDay = "";
                if(datesList.get(i).getDay() == 1){sDay = "Monday";}
                if(datesList.get(i).getDay() == 2){sDay = "Tuesday";}
                if(datesList.get(i).getDay() == 3){sDay = "Wednesday";}
                if(datesList.get(i).getDay() == 4){sDay = "Thursday";}
                if(datesList.get(i).getDay() == 5){sDay = "Friday";}
                timetable.addDay(datesList.get(i).getDate(),datesList.get(i).getMonth(),datesList.get(i).getYear(), sDay );
            }

        return timetable;
    }
    private ArrayList<Hall> hallsAvailability(String sTimeStart, String sTimeEnd) throws SQLException, ParseException {
        int iCode =0;
        ArrayList<Hall> hallsList = new ArrayList<>();
        String sql77 = "SELECT inside_code, capacity, intended_for_lectures FROM FACILITIES";
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(sql77);
        while(resultSet.next()){
            if(resultSet.getInt(3) == 0) {
                iCode++;
                hallsList.add(new Hall(connection, resultSet.getInt("capacity"), resultSet.getString("inside_code"), sTimeStart, sTimeEnd, iCode));
            }
        }
        return hallsList;
    }private ArrayList<Hall> hallsAvailability_forlectures(String sTimeStart, String sTimeEnd) throws SQLException, ParseException {
        int iCode =0;
        ArrayList<Hall> hallsList = new ArrayList<>();
        String sql77 = "SELECT inside_code, capacity, intended_for_lectures FROM FACILITIES";
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(sql77);
        while(resultSet.next()){
            if(resultSet.getInt(3) == 1) {
                iCode++;
                hallsList.add(new Hall(connection, resultSet.getInt("capacity"), resultSet.getString("inside_code"), sTimeStart, sTimeEnd, iCode));
            }
        }
        return hallsList;
    }
    /**/


    /**/
    public String getS_mostRecentDate() {
        return s_mostRecentDate;
    }
    public void setS_mostRecentDate(String s_mostRecentDate) {
        this.s_mostRecentDate = s_mostRecentDate;
    }
    public static String getS_lastmostRecentDate() {
        return s_lastmostRecentDate;
    }
    public void addOneDayToLastMostRecentDay() throws ParseException {
        String sMost = getS_mostRecentDate();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new SimpleDateFormat("dd-MMM-yyyy").parse(sMost));
        calendar.add(Calendar.DATE, 1);
    }
    public static void setS_lastmostRecentDate(String s_lastmostRecentDate) {
        GreedyAlgorithm.s_lastmostRecentDate = s_lastmostRecentDate;
    }
    /**/
    private Double d_TotalHoursOfHallsAvailable_Bush_House(String sStardDate, String sEndDate) throws ParseException, SQLException {

        double dHours = 0.0;
        Date date1 = new SimpleDateFormat("dd-MMM-yyyy").parse(sStardDate);
        Date date2 = new SimpleDateFormat("dd-MMM-yyyy").parse(sEndDate);
        String sql34 = "";
        PreparedStatement prst;
        if(date1.getMonth() != date2.getMonth() && date2.getMonth() - date1.getMonth() == 1){
            sql34 = " SELECT COUNT(*) FROM two_weeks_availability_halls_bush_house WHERE AVAILABLE = 1 AND MONTH = ? AND DATE >= ? " +
                    " UNION " +
                    " SELECT COUNT(*) FROM two_weeks_availability_halls_bush_house WHERE AVAILABLE = 1 AND MONTH = ? AND DATE <= ? ";
            prst = connection.prepareStatement(sql34);
            prst.setInt(1, date1.getMonth());
            prst.setInt(2, date1.getDate());
            prst.setInt(3, date2.getMonth());
            prst.setInt(4,date2.getDate());
        }
        else {
            sql34 = " SELECT COUNT(*) FROM two_weeks_availability_halls_bush_house WHERE AVAILABLE = 1 AND MONTH = ? AND DATE >= ? UNION";
            int iCounter = 1;
            int iSecondCounter = 0;

            for (int i = date1.getMonth() + 1; i < date2.getMonth(); i++) {
                sql34 += " SELECT COUNT(*) FROM two_weeks_availability_halls_bush_house WHERE AVAILABLE = 1 AND MONTH = ? UNION ";
                iSecondCounter++;
            }
            sql34 += " SELECT COUNT(*) FROM two_weeks_availability_halls_bush_house WHERE AVAILABLE = 1 AND MONTH = ? AND DATE <= ?";


            prst = connection.prepareStatement(sql34);
            prst.setInt(iCounter, date1.getMonth());
            prst.setInt(++iCounter, date1.getDate());
            for (int i = 1; i < iSecondCounter + 1; i++) {
                prst.setInt(++iCounter, date2.getMonth() - i);
            }
            prst.setInt(++iCounter, date2.getMonth()); //check this
            prst.setInt(++iCounter, date2.getDate());    //check this

        }
        ResultSet rst = prst.executeQuery();
        double dSum = 0.0;
        while (rst.next()){
            dSum += rst.getInt(1);
        }


        return dSum/2;
    }
    private Double d_TotalHoursOfHallsAvailable_Waterloo(String sStardDate, String sEndDate) throws ParseException, SQLException {
        double dHours = 0.0;
        Date date1 = new SimpleDateFormat("dd-MMM-yyyy").parse(sStardDate);
        Date date2 = new SimpleDateFormat("dd-MMM-yyyy").parse(sEndDate);
        String sql34 = "";
        PreparedStatement prst;
        if(date1.getMonth() != date2.getMonth() && date2.getMonth() - date1.getMonth() == 1){
            sql34 = " SELECT * FROM two_weeks_availability_halls_waterloo WHERE AVAILABLE = 1 AND MONTH = ? AND DATE >= ? " +
                    " UNION " +
                    " SELECT * FROM two_weeks_availability_halls_waterloo WHERE AVAILABLE = 1 AND MONTH = ? AND DATE <= ? ";
            prst = connection.prepareStatement(sql34);
            prst.setInt(1, date1.getMonth());
            prst.setInt(2, date1.getDate());
            prst.setInt(3, date2.getMonth());
            prst.setInt(4,date2.getDate());
        }
        else {
            sql34 = " SELECT COUNT(*) FROM two_weeks_availability_waterloo WHERE AVAILABLE = 1 AND MONTH = ? AND DATE >= ? UNION";
            int iCounter = 1;
            int iSecondCounter = 0;

            for (int i = date1.getMonth() + 1; i < date2.getMonth(); i++) {
                sql34 += " SELECT COUNT(*) FROM two_weeks_availability_waterloo WHERE AVAILABLE = 1 AND MONTH = ? UNION ";
                iSecondCounter++;
            }
            sql34 += " SELECT COUNT(*) FROM two_weeks_availability_waterloo WHERE AVAILABLE = 1 AND MONTH = ? AND DATE <= ?";


            prst = connection.prepareStatement(sql34);
            prst.setInt(iCounter, date1.getMonth());
            prst.setInt(++iCounter, date1.getDate());
            for (int i = 1; i < iSecondCounter + 1; i++) {
                prst.setInt(++iCounter, date2.getMonth() - i);
            }
            prst.setInt(++iCounter, date2.getMonth()); //check this
            prst.setInt(++iCounter, date2.getDate());    //check this

        }
        ResultSet rst = prst.executeQuery();
        double dSum = 0.0;
        while (rst.next()){
            dSum += rst.getInt(1);
        }


        return dSum/2;
    }
    private Double d_TotalHoursOfHallsAvailable_Waterloo_SpecificHall(String sName, String sStardDate, String sEndDate) throws ParseException, SQLException {
        double dHours = 0.0;
        Date date1 = new SimpleDateFormat("dd-MMM-yyyy").parse(sStardDate);
        Date date2 = new SimpleDateFormat("dd-MMM-yyyy").parse(sEndDate);
        String sql34 = "";
        PreparedStatement prst;
        if(date1.getMonth() != date2.getMonth() && date2.getMonth() - date1.getMonth() == 1){
            sql34 = " SELECT COUNT(*) FROM two_weeks_availability_halls_waterloo WHERE AVAILABLE = 1 AND MONTH = ? AND DATE >= ? AND HALL = ?" +
                    " UNION " +
                    " SELECT COUNT(*) FROM two_weeks_availability_halls_waterloo WHERE AVAILABLE = 1 AND MONTH = ? AND DATE <= ? AND HALL = ?";
            prst = connection.prepareStatement(sql34);
            prst.setInt(1, date1.getMonth());
            prst.setInt(2, date1.getDate());
            prst.setString(3, sName);
            prst.setInt(4, date2.getMonth());
            prst.setInt(5,date2.getDate());
            prst.setString(6, sName);
        }
        else {
            sql34 = " SELECT COUNT(*) FROM two_weeks_availability_halls_waterloo WHERE AVAILABLE = 1 AND MONTH = ? AND DATE >= ? AND HALL = ? UNION";
            int iCounter = 1;
            int iSecondCounter = 0;

            for (int i = date1.getMonth() + 1; i < date2.getMonth(); i++) {
                sql34 += " SELECT COUNT(*) FROM two_weeks_availability_halls_waterloo WHERE AVAILABLE = 1 AND MONTH = ? AND HALL = ? UNION ";
                iSecondCounter++;
            }
            sql34 += " SELECT COUNT(*) FROM two_weeks_availability_halls_waterloo WHERE AVAILABLE = 1 AND MONTH = ? AND DATE <= ? AND HALL = ? ";


            prst = connection.prepareStatement(sql34);
            prst.setInt(iCounter, date1.getMonth());
            prst.setInt(++iCounter, date1.getDate());
            prst.setString(++iCounter, sName);
            for (int i = 1; i < iSecondCounter + 1; i++) {
                prst.setInt(++iCounter, date2.getMonth() - i);
                prst.setString(++iCounter, sName);
            }
            prst.setInt(++iCounter, date2.getMonth()); //check this
            prst.setInt(++iCounter, date2.getDate());    //check this
            prst.setString(++iCounter, sName);
        }
        ResultSet rst = prst.executeQuery();
        double dSum = 0.0;
        while (rst.next()){
            dSum += rst.getInt(1);
        }


        return dSum/2;
    }

    private Double d_TotalHoursOfHallsAvailable(String sStardDate, String sEndDate) throws ParseException, SQLException {
        return d_TotalHoursOfHallsAvailable_Bush_House(sStardDate,sEndDate) + d_TotalHoursOfHallsAvailable_Waterloo(sStardDate, sEndDate);
    }
    private int d_AllHallsCapacity() throws  SQLException{
        int iResult = 0;
        String sql99 = "SELECT SUM(CAPACITY) FROM FACILITIES";
        PreparedStatement prst = connection.prepareStatement(sql99);
        ResultSet rst = prst.executeQuery();
        while(rst.next()){
            iResult = rst.getInt(1);
        }
        return iResult;
    }
    private int d_SpicificHallCapacity(String sName) throws  SQLException{
        int iResult = 0;
        String sql99 = "SELECT CAPACITY FROM FACILITIES WHERE inside_code = ?";
        PreparedStatement prst = connection.prepareStatement(sql99);
        prst.setString(1, sName);
        ResultSet rst = prst.executeQuery();
        while(rst.next()){
            iResult = rst.getInt(1);
        }
        return iResult;
    }
    /**/
    private double d_capacityPerHour_Waterloo_SpecificHall(String sName,String sStartDate,String sEndDate) throws SQLException, ParseException {
        double iResult = d_SpicificHallCapacity(sName);
        double sHours = d_TotalHoursOfHallsAvailable_Waterloo_SpecificHall(sName, sStartDate, sEndDate);

        return sHours*iResult / 55;
    }
    private int countStudents(int iYear) throws SQLException {
        int iResult = 0;
        String sql77 = "";
        if(iYear == 3) sql77 = "SELECT COUNT(*) FROM S_STUDENTS WHERE KINGS_ID LIKE '18%'";
        else if(iYear == 2) sql77 = "SELECT COUNT(*) FROM S_STUDENTS WHERE KINGS_ID LIKE '19%'";
        else if(iYear == 1 )sql77 = "SELECT COUNT(*) FROM S_STUDENTS WHERE KINGS_ID LIKE '20%'";
        else System.out.println("Invalid Year");
        Statement statement = connection.createStatement();
        ResultSet rst = statement.executeQuery(sql77);
        while (rst.next()){
            iResult = rst.getInt(1);
        }
        rst.close();
        return iResult;
    }

    private int countStudentsAbbreviation(String sName, String sTable) throws SQLException {
        int iResult = 0;
        String sql77 = "";
        sql77 = "SELECT COUNT(*) FROM " + sTable + " WHERE ";
        Statement statement = connection.createStatement();
        ResultSet rst = statement.executeQuery(sql77);
        while (rst.next()){
            iResult = rst.getInt(1);
        }
        rst.close();
        return iResult;
    }


    private ArrayList<Hall> searchForAHall(ArrayList<Hall> halls,int iCapacity) {
        ArrayList<Hall> suitable = new ArrayList<>();
        for(int i = 0; i < halls.size(); i++){
            if(halls.get(i).getiCapacity() >= iCapacity){
                suitable.add(halls.get(i));
            }
        }
        return  suitable;
    }

    //

    public  Week_Timetable recursion(Week_Timetable timetable, ArrayList<Hall> halls, ArrayList<Duplet> duplets,ArrayList<Duplet> assigned, int iNumStudents,int inewNumStudents, int iIndicatorOne, int iIndicatorTwo){
        //indicatortwo would be for the number of times this recursion has been called, starts with 0, first time it tries
        //to find a hall for 1/2 of the students it would be called once,hence value 1, and so on.
        //if it has been called once, that would mean we have split the lecture into two slots,
        //so one lecture would be assigned and one would be thrown into the end of the queue
        // (is this a good idea? ) is it better to assign the both lectures after the split?
        int lectureAssigned = 0;
        int Students;
        Duplet temp = null;
        Week_Timetable myTimetable = timetable;
        if(iIndicatorOne == 1 && (inewNumStudents > 2) && myTimetable.getLectures().size() != 0) {//switch to iNewNum
            ArrayList<Hall> newhalls = searchForAHall(halls, inewNumStudents);
            if (newhalls.isEmpty() ){
                if(iNumStudents %2 == 0)
                    myTimetable = recursion(myTimetable,myTimetable.getHalls(), myTimetable.getLectures(), myTimetable.getAssignedLectures(),
                            iNumStudents, iNumStudents / 2 , iIndicatorOne, ++iIndicatorTwo);// + 1 here because a lecture could be attendedby an odd number of students
                else
                    myTimetable = recursion(myTimetable,myTimetable.getHalls(), myTimetable.getLectures(), myTimetable.getAssignedLectures(),
                            iNumStudents, iNumStudents / 2 + 1 , iIndicatorOne, ++iIndicatorTwo);// + 1 here because a lecture could be attendedby an odd number of students
            }
            else {
                try {
                    temp = (Duplet) duplets.get(0).clone();
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
                temp.setiNumberOfStudentsAttending(inewNumStudents); //changes here
                PreferredDays preferredDays = temp.getPreferredDays();
                String str = preferredDays.getPrefDay().get(0);
                int prefdHour = preferredDays.getiPrefHour();
                int prefdH2 = preferredDays.getiPrefHour_2();
                double duration = temp.getiHours();

                //HERE FOR EVERY AVAILABLE HALL CHECK FOR A PREFERRED TIME FROM THE LECTURE'S PREFERENCES

                for (int k = 0; k < newhalls.size(); k++) {
                    if (lectureAssigned == 1) {
                        break;
                    }
                    //AS WE SAID THERE ARE PREFERENCE ATTACHED TO EVERY LECTURE (EVEN THOUGH THEY COULD BE MISSING HENCE = 0)
                    //THIS LOOP COMPARES EVERY PREFERENCE FOR A DAY + TIME FROM THE ARRAY OF PREFERRED DAYS INSIDE LECTURE

                    for (int i = 0; i < temp.getPreferredDays().getPrefDay().size(); i++) {

                        //EVERY LECTURE HAS TWO MOST PREFERRED HOURS, I CHECK FOR AVAILABILITY HERE

                        int iResult = newhalls.get(k).getAvailability(prefdHour, (int) (prefdHour + duration * 100), preferredDays.getPrefDay().get(i));
                        int iResult2 = newhalls.get(k).getAvailability(prefdH2, (int) (prefdH2 + duration * 100), preferredDays.getPrefDay().get(i));

                        if (iResult == 1 || iResult2 == 1) {

                            if (iResult == 1) {

                            } else {
                                prefdHour = prefdH2;            //IF FIRST PREFD HOUR IS NOT AVAILABLE, USE THE SECOND ONE
                            }

                            int hallCode = newhalls.get(k).getiAdditionalCode();                // every hall has a code to it, distinguishing it from the others, to update the real array of halls, I need to get the code from the available halls and find it in the bigger array
                            for (int z = 0; z < myTimetable.getHalls().size(); z++) {           // traversing the bigger array, actual array of halls
                                if (myTimetable.getHalls().get(z).getiAdditionalCode() == hallCode) {

                                    myTimetable.getHalls().get(z).setAvToZero(prefdHour, (int) (prefdHour + duration * 100), preferredDays.getPrefDay().get(i));            //set the actual hall to unavailable
                                    //if(iIndicatorTwo != 0) { myTimetable.getLectures().add(temp) ; }  //put brackets here

                                    lectureAssigned = 1; //assigned here,  exit
                                    myTimetable.getLectures().remove(0);
                                    temp.setiNumberOfStudentsAttending(inewNumStudents);
                                    myTimetable.getAssignedLectures().add(temp);      //this is implemented on the following lines below

                                    for (int u = 0; u < myTimetable.getWeekTimet().size(); u++) {
                                        if (myTimetable.getDay(u).getSname().toLowerCase(Locale.ROOT).equals(preferredDays.getPrefDay().get(i).toLowerCase(Locale.ROOT))) {
                                            myTimetable.getDay(u).v_assignEvent(prefdHour, duration, newhalls.get(k).getsAbbrev(), temp.getsLect(), "event");
                                        }

                                    }

                                    if (iIndicatorTwo != 0) {
                                        for (int s = 0; s < Math.ceil(iNumStudents / inewNumStudents) - 1; s++) {// - 1
                                            myTimetable.getLectures().add(temp);
                                        } //else here ??!!
                                    }
                                    myTimetable.setPreviousRecursionWorked(1);

                                    return myTimetable;

                                }
                            }
                            //there would need to be a class taking note of the days with lectures assigned Week_Timetable possibly
                        }
                    }

                }


                for (int j = 0; j < newhalls.size(); j++) {
                    if (lectureAssigned == 1) {
                        break;
                    }
                    //THIS IS IF WE FIND A SUITABLE TIME IN THESE HALLS, WHAT IF DO NOT??
                    for (int x = 0; x < temp.getPreferredDays().getPrefDay().size(); x++) {

                        int data = newhalls.get(j).findAvailableSlot_PreferredDay(900, (int) duration, myTimetable.getLectures().get(0).getPreferredDays().getPrefDay().get(x));//check firs findAvailableSlot_PreferredDay

                        if (data != 0) {
                            //first available position found. Should it be used in this fashion? Probably suitable for GreedyAlg
                            int iAddCode = newhalls.get(j).getiAdditionalCode();
                            for (int t = 0; t < myTimetable.getHalls().size(); t++) {
                                if (myTimetable.getHalls().get(t).getiAdditionalCode() == iAddCode) {
                                    timetable.getHalls().get(t).setAvToZero(data, (int) (data + duration * 100), myTimetable.getLectures().get(0).getPreferredDays().getPrefDay().get(x));   //CHECK AVTOZERO
                                    //add to assigned lectures ?
                                    //assign to a day as well !
                                    if (iIndicatorTwo != 0) {
                                        for (int z = 0; z < Math.ceil(iNumStudents / inewNumStudents) - 1; z++) {
                                            myTimetable.getLectures().add(temp);
                                        }
                                    }
                                    lectureAssigned = 1; //assigned here,  exit
                                    myTimetable.setPreviousRecursionWorked(1);
                                    myTimetable.getLectures().remove(0);
                                    myTimetable.getAssignedLectures().add(temp);//assigned.add, quick fix

                                    for (int u = 0; u < myTimetable.getWeekTimet().size(); u++) {
                                        if (myTimetable.getWeekTimet().get(u).getSname().toLowerCase(Locale.ROOT).equals(temp.getPreferredDays().getPrefDay().get(x).toLowerCase(Locale.ROOT))) {
                                            myTimetable.getDay(u).v_assignEvent(data, duration, newhalls.get(j).getsAbbrev(), temp.getsLect(), "event");
                                        }
                                    }
                                    return myTimetable;  //MORE DUPLETS MIGHT NEED TO BE ADDED, HOW DO I PROCEED HERE ?;
                                }
                            }
                        }
                    }
                }


                for (int j = 0; j < newhalls.size(); j++) {
                    //THIS IS IF WE FIND A SUITABLE TIME IN THESE HALLS, WHAT IF NOT??
                    if (lectureAssigned == 1) {
                                    break;
                    }
                        CoupledData data = newhalls.get(j).findAvailableSlot(900, (int) duration);//check firs findAvailableSlot_PreferredDay

                        if (!data.getsDay().isEmpty() && data.getiHour() != 0) {
                            //first available position found. Should it be used in this fashion? Probably suitable for GreedyAlg
                            int iAddCode = newhalls.get(j).getiAdditionalCode();
                            for (int t = 0; t < myTimetable.getHalls().size(); t++) {//CHANGE TO MYTABLE.GETHALLS
                                if (myTimetable.getHalls().get(t).getiAdditionalCode() == iAddCode) {
                                    timetable.getHalls().get(t).setAvToZero(data.getiHour(), (int) (data.getiHour() + duration * 100), data.getsDay());   //CHECK AVTOZERO
                                    //add to assigned lectures ?
                                    //assign to a day as well !

                                    if (iIndicatorTwo != 0) {
                                        for (int z = 0; z < Math.ceil(iNumStudents / inewNumStudents) - 1; z++) { //SET Z < .. - 1
                                            myTimetable.getLectures().add(temp);
                                        }
                                    }

                                    lectureAssigned = 1; //assigned here,  exit
                                    myTimetable.setPreviousRecursionWorked(1);
                                    myTimetable.getLectures().remove(0);
                                    myTimetable.getAssignedLectures().add(temp);//assigned.add, quick fix

                                    for (int f = 0; f < myTimetable.getWeekTimet().size(); f++) {
                                        if (myTimetable.getWeekTimet().get(f).getSname().toLowerCase(Locale.ROOT).equals(data.getsDay().toLowerCase(Locale.ROOT))) {
                                            myTimetable.getWeekTimet().get(f).v_assignEvent(data.getiHour(), duration, newhalls.get(j).getsAbbrev(), temp.getsLect(), "event");
                                        }
                                    }
                                    return myTimetable;
                                }
                            }
                        }
                    }


                    if (lectureAssigned == 0 && myTimetable.getLectures().size() != 0) {


                        //System.out.println("No solution found, weirdly enough");
                        // maybe explore the possibility of using the unused or remaining halls somehow even if
                        //  assign randomly to a different timeslot
                        //  call the algorithm again with half the attending students

                        /* UP FOR TESTING --> SEEMS TO WORK */

                        ///.............

                        if (iNumStudents % 2 == 0) {
                            myTimetable = recursion(myTimetable, myTimetable.getHalls(), myTimetable.getLectures(), myTimetable.getAssignedLectures(),
                                    iNumStudents, inewNumStudents / 2, iIndicatorOne, ++iIndicatorTwo);// + 1 here because a lecture could be attendedby an odd number of students
                        } else {
                            myTimetable = recursion(myTimetable, myTimetable.getHalls(), myTimetable.getLectures(), myTimetable.getAssignedLectures(),
                                    iNumStudents, inewNumStudents / 2 + 1, iIndicatorOne, ++iIndicatorTwo);// + 1 here because a lecture could be attendedby an odd number of students
                        }
                        ///.............


                    }

                }
            }


        else {
            try {
                temp = (Duplet) myTimetable.getLectures().get(0).clone();
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
            myTimetable.getLectures().remove(0);
            myTimetable.getLectures().add(temp);
        }
                return myTimetable;
            //changes made involve moving the last two for loops inside the newhalls.size() array
            // + I commented out the if statement after that same loop is done
    }



    public  Week_Timetable recursion2(Week_Timetable timetable, ArrayList<Hall> halls, ArrayList<Duplet> duplets,ArrayList<Duplet> assigned, int iNumStudents,int inewNumStudents, int iIndicatorOne, int iIndicatorTwo, int arbitraryNumber){

        int lectureAssigned = 0;
        int Students;
        Duplet temp = null;
        Week_Timetable myTimetable = timetable;
        if(iIndicatorOne == 1 && (inewNumStudents > 2) && myTimetable.getLectures().size() != 0) {//switch to iNewNum
            ArrayList<Hall> newhalls = searchForAHall(halls, inewNumStudents);
            if (newhalls.isEmpty() ){
                if(iNumStudents %2 == 0)
                    myTimetable = recursion2(myTimetable,myTimetable.getHalls(), myTimetable.getLectures(), myTimetable.getAssignedLectures(),
                            iNumStudents, inewNumStudents - arbitraryNumber, iIndicatorOne, ++iIndicatorTwo,arbitraryNumber);// + 1 here because a lecture could be attendedby an odd number of students
                else
                    myTimetable = recursion2(myTimetable,myTimetable.getHalls(), myTimetable.getLectures(), myTimetable.getAssignedLectures(),
                            iNumStudents, inewNumStudents - arbitraryNumber, iIndicatorOne, ++iIndicatorTwo,arbitraryNumber);// + 1 here because a lecture could be attendedby an odd number of students
            }
            else{
                try {
                    temp = (Duplet) duplets.get(0).clone();
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
                temp.setiNumberOfStudentsAttending(inewNumStudents); //changes here
                PreferredDays preferredDays = temp.getPreferredDays();
                String str = preferredDays.getPrefDay().get(0);
                int prefdHour = preferredDays.getiPrefHour();
                int prefdH2 = preferredDays.getiPrefHour_2();
                double duration = temp.getiHours();

                //HERE FOR EVERY AVAILABLE HALL CHECK FOR A PREFERRED TIME FROM THE LECTURE'S PREFERENCES

                for(int k = 0; k < newhalls.size(); k++) {
                    if (lectureAssigned == 1) {
                        break;
                    }
                    //AS WE SAID THERE ARE PREFERENCE ATTACHED TO EVERY LECTURE (EVEN THOUGH THEY COULD BE MISSING HENCE = 0)
                    //THIS LOOP COMPARES EVERY PREFERENCE FOR A DAY + TIME FROM THE ARRAY OF PREFERRED DAYS INSIDE LECTURE

                    for(int i = 0; i < temp.getPreferredDays().getPrefDay().size(); i++){

                        //EVERY LECTURE HAS TWO MOST PREFERRED HOURS, I CHECK FOR AVAILABILITY HERE

                        int iResult = newhalls.get(k).getAvailability(prefdHour, (int) (prefdHour + duration * 100), preferredDays.getPrefDay().get(i));
                        int iResult2 = newhalls.get(k).getAvailability(prefdH2, (int) (prefdH2 + duration * 100), preferredDays.getPrefDay().get(i));

                        if (iResult == 1 || iResult2 == 1) {

                            if (iResult == 1) {

                            } else {
                                prefdHour = prefdH2;            //IF FIRST PREFD HOUR IS NOT AVAILABLE, USE THE SECOND ONE
                            }

                            int hallCode = newhalls.get(k).getiAdditionalCode();                // every hall has a code to it, distinguishing it from the others, to update the real array of halls, I need to get the code from the available halls and find it in the bigger array
                            int iDate = 0;
                            int iMonth = 0;
                            int iYear = 0;
                            String prefDay = "";

                            for (int z = 0; z < myTimetable.getHalls().size(); z++) {           // traversing the bigger array, actual array of halls
                                if (myTimetable.getHalls().get(z).getiAdditionalCode() == hallCode) {

                                    myTimetable.getHalls().get(z).setAvToZero(prefdHour, (int) (prefdHour + duration * 100), preferredDays.getPrefDay().get(i));            //set the actual hall to unavailable
                                    //if(iIndicatorTwo != 0) { myTimetable.getLectures().add(temp) ; }  //put brackets here

                                    lectureAssigned = 1; //assigned here,  exit
                                    temp.setiNumberOfStudentsAttending(inewNumStudents);
                                    myTimetable.getAssignedLectures().add(temp);      //this is implemented on the following lines below

                                    for(int u = 0; u < myTimetable.getWeekTimet().size(); u++){
                                        if(myTimetable.getDay(u).getSname().toLowerCase(Locale.ROOT).equals(preferredDays.getPrefDay().get(i).toLowerCase(Locale.ROOT))){
                                            myTimetable.getDay(u).v_assignEvent(prefdHour,duration,newhalls.get(k).getsAbbrev(), temp.getsLect(),"event");
                                            prefDay = myTimetable.getDay(u).getSname();
                                            iDate = myTimetable.getDay(u).getiDate();
                                            iMonth = myTimetable.getDay(u).getiMonth();
                                            iYear = myTimetable.getDay(u).getiYear();
                                        }

                                    }

                                    if (iIndicatorTwo != 0) {
                                        try {

                                            Duplet duplet = (Duplet) temp.clone();
                                            duplet.setiHourScheduled(prefdHour);
                                            duplet.setiDayScheduled(iDate);
                                            duplet.setiMonthScheduled(iMonth);
                                            duplet.setiYearScheduled(iYear);
                                            duplet.setsDayOfWeek(prefDay);
                                            duplet.setiNumberOfStudentsAttending(myTimetable.getLectures().get(0).getiNumberOfStudentsAttending() - inewNumStudents);
                                            myTimetable.getLectures().add(duplet);

                                        } catch (CloneNotSupportedException e) {
                                            e.printStackTrace();
                                        }

                                    }
                                    myTimetable.setPreviousRecursionWorked(1);
                                    myTimetable.getLectures().remove(0);

                                    return myTimetable;

                                }
                            }
                            //there would need to be a class taking note of the days with lectures assigned Week_Timetable possibly
                        }
                    }

                }

                //THIS IS IF WE FIND A SUITABLE TIME IN THESE HALLS

                for (int j = 0; j < newhalls.size(); j++) {

                    for(int x = 0; x < temp.getPreferredDays().getPrefDay().size(); x++) {

                        int data = newhalls.get(j).findAvailableSlot_PreferredDay(900, (int) duration ,myTimetable.getLectures().get(0).getPreferredDays().getPrefDay().get(x));//check firs findAvailableSlot_PreferredDay
                        int iDate = 0;
                        int iMonth = 0;
                        int iYear = 0;

                        if (data != 0 ) {
                            //first available position found. Should it be used in this fashion? Probably suitable for GreedyAlg
                            int iAddCode = newhalls.get(j).getiAdditionalCode();
                            for (int t = 0; t < myTimetable.getHalls().size(); t++) {
                                if (myTimetable.getHalls().get(t).getiAdditionalCode() == iAddCode) {
                                    timetable.getHalls().get(t).setAvToZero(data, (int) (data + duration * 100),myTimetable.getLectures().get(0).getPreferredDays().getPrefDay().get(x) );   //CHECK AVTOZERO
                                    //add to assigned lectures ?
                                    //assign to a day as well !

                                    for(int u = 0; u < myTimetable.getWeekTimet().size(); u++){
                                        if(myTimetable.getWeekTimet().get(u).getSname().toLowerCase(Locale.ROOT).equals(temp.getPreferredDays().getPrefDay().get(x).toLowerCase(Locale.ROOT))){
                                            myTimetable.getDay(u).v_assignEvent(data,duration,newhalls.get(j).getsAbbrev(), temp.getsLect(), "event");
                                            iDate = myTimetable.getWeekTimet().get(u).getiDate();
                                            iMonth = myTimetable.getWeekTimet().get(u).getiMonth();
                                            iYear = myTimetable.getWeekTimet().get(u).getiYear();
                                        }
                                    }

                                    if (iIndicatorTwo != 0) {
                                        try {

                                            Duplet duplet = (Duplet) temp.clone();
                                            duplet.setiNumberOfStudentsAttending(myTimetable.getLectures().get(0).getiNumberOfStudentsAttending() - inewNumStudents);
                                            duplet.setiHourScheduled(data);
                                            duplet.setsDayOfWeek(temp.getPreferredDays().getPrefDay().get(x));
                                            duplet.setiDayScheduled(iDate);
                                            duplet.setiMonthScheduled(iMonth);
                                            duplet.setiYearScheduled(iYear);
                                            myTimetable.getLectures().add(duplet);

                                        } catch (CloneNotSupportedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    lectureAssigned = 1; //assigned here,  exit
                                    myTimetable.setPreviousRecursionWorked(1);
                                    myTimetable.getLectures().remove(0);
                                    myTimetable.getAssignedLectures().add(temp);//assigned.add, quick fix


                                    return myTimetable;  //MORE DUPLETS MIGHT NEED TO BE ADDED, HOW DO I PROCEED HERE ?;
                                }
                            }
                        }
                    }
                }



                for (int j = 0; j < newhalls.size(); j++) {
                    //THIS IS IF WE FIND A SUITABLE TIME IN THESE HALLS, WHAT IF NOT??

                    int iDate = 0;
                    int iMonth = 0;
                    int iYear = 0;
                    CoupledData data = newhalls.get(j).findAvailableSlot(900, (int) duration);//check firs findAvailableSlot_PreferredDay

                    if (!data.getsDay().isEmpty() && data.getiHour() != 0) {
                        //first available position found. Should it be used in this fashion? Probably suitable for GreedyAlg
                        int iAddCode = newhalls.get(j).getiAdditionalCode();

                        for(int t =0; t < myTimetable.getHalls().size(); t++){//CHANGE TO MYTABLE.GETHALLS
                            if(myTimetable.getHalls().get(t).getiAdditionalCode() == iAddCode){
                                timetable.getHalls().get(t).setAvToZero(data.getiHour(), (int)(data.getiHour() + duration*100), data.getsDay());   //CHECK AVTOZERO
                                //add to assigned lectures ?
                                //assign to a day as well !


                                for(int f = 0; f < myTimetable.getWeekTimet().size(); f++){
                                    if(myTimetable.getWeekTimet().get(f).getSname().toLowerCase(Locale.ROOT).equals(data.getsDay().toLowerCase(Locale.ROOT))) {
                                        myTimetable.getWeekTimet().get(f).v_assignEvent(data.getiHour(), duration, newhalls.get(j).getsAbbrev(),  temp.getsLect(), "event");
                                        iDate = myTimetable.getWeekTimet().get(f).getiDate();
                                        iMonth = myTimetable.getWeekTimet().get(f).getiMonth();
                                        iYear = myTimetable.getWeekTimet().get(f).getiYear();
                                    }
                                }


                                if(iIndicatorTwo != 0) {
                                    try {

                                        Duplet duplet = (Duplet) temp.clone();
                                        duplet.setiNumberOfStudentsAttending(myTimetable.getLectures().get(0).getiNumberOfStudentsAttending() - inewNumStudents);
                                        duplet.setiHourScheduled(data.getiHour());
                                        duplet.setsDayOfWeek(data.getsDay());
                                        duplet.setiDayScheduled(iDate);
                                        duplet.setiMonthScheduled(iMonth);
                                        duplet.setiYearScheduled(iYear);
                                        myTimetable.getLectures().add(duplet);

                                    } catch (CloneNotSupportedException e) {
                                        e.printStackTrace();
                                    }
                                }

                                lectureAssigned = 1; //assigned here,  exit
                                myTimetable.setPreviousRecursionWorked(1);
                                myTimetable.getLectures().remove(0);
                                myTimetable.getAssignedLectures().add(temp);//assigned.add, quick fix

                                return myTimetable;
                            }
                        }
                    }
                }



                if(lectureAssigned == 0 && myTimetable.getLectures().size() != 0 ){


                    //System.out.println("No solution found, weirdly enough");
                    // maybe explore the possibility of using the unused or remaining halls somehow even if
                    //  assign randomly to a different timeslot
                    //  call the algorithm again with half the attending students

                    /* UP FOR TESTING --> SEEMS TO WORK */

                    ///.............

                    if(iNumStudents %2 == 0) {
                        myTimetable = recursion2(myTimetable, myTimetable.getHalls(), myTimetable.getLectures(), myTimetable.getAssignedLectures(),
                                iNumStudents, inewNumStudents - arbitraryNumber, iIndicatorOne, ++iIndicatorTwo, arbitraryNumber);// + 1 here because a lecture could be attendedby an odd number of students
                    }else {
                        myTimetable = recursion2(myTimetable, myTimetable.getHalls(), myTimetable.getLectures(), myTimetable.getAssignedLectures(),
                                iNumStudents, inewNumStudents -arbitraryNumber , iIndicatorOne, ++iIndicatorTwo,arbitraryNumber);// + 1 here because a lecture could be attendedby an odd number of students
                    }
                    ///.............


                }

            }

        }

        else {
            try {
                temp = (Duplet) myTimetable.getLectures().get(0).clone();
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
            myTimetable.getLectures().remove(0);
            myTimetable.getLectures().add(temp);
        }
        return myTimetable;
        //changes made involve moving the last two for loops inside the newhalls.size() array
        // + I commented out the if statement after that same loop is done
    }



    //generate a table for availability and a table for assigning lectures and maybe start updating them
    //greedy algorithm here would mean pick up the optimal choice at each step in an attempt to find the best solution for a schedule
    //however this of course is not the expected top algorithm to find the top solution



    // Sort out the heuristics

    //for each course we need the heuristics to define a greedy plan
    //also this plan needs to follow constraints
    //and possibly I need a validator that the plan is correct // compare with the use of constraints

    public int generateGreedySolution(String sTable, int min, int max) throws SQLException, ParseException {

        Week_Timetable week_timetableont = myTimetable(7);
        Week_Timetable week_timetabletwo = myTimetable(7);

//        week_timetableont.v_print();
//        week_timetabletwo.v_print();

        ArrayList <Duplet> assignedLectures = new ArrayList<>();
        ArrayList <Duplet> updatedLectures = new ArrayList<>();

        //ArrayList <Duplet> myarr2 = lecturesToBeAssigned(1, sTable,2019);  //this ARRAY contains all the info for the lectures
        ArrayList <Duplet> myarr = lecturesToBeAssigned2( getTwoInts() ,1, sTable);  //this ARRAY contains all the info for the lectures
        ArrayList <Duplet> weekOne = new ArrayList<>();
        ArrayList <Duplet> weekTwo = new ArrayList<>();

        for(int i = 0; i < myarr.size(); i++){
            myarr.get(i).setPreferredDays( o_getPreferencesStudents(    myarr.get(i).getsLect() , 1  ,5));
            myarr.get(i).setsTeachersPreference( s_getLecturersChoice(    myarr.get(i).getsLect()   ));
          //  myarr.get(i).print();
        }

        Collections.sort(myarr);                        //sort my array to start searching for the lectures with least students (asc order)

        for(int i = 0; i < myarr.size(); i++){
            if(i % 2 == 0){ weekOne.add(myarr.get(i));}
            else{weekTwo.add(myarr.get(i));}
        }


         week_timetableont.setLectures(weekOne);
//        week_timetabletwo.setLectures(weekTwo);
//        week_timetableont.setLectures(myarr);

        //System.out.println(weekOne.size() + " " + weekTwo.size());


        //only to check on which is available when
        //ArrayList<Hall> halls = hallsAvailability("28-Sep-2020", "04-Oct-2020");
        ArrayList<Hall> halls = hallsAvailability_forlectures("28-Sep-2020", "04-Oct-2020");
        //halls.forEach((n)->System.out.println(n));
        week_timetableont.setHalls(halls);



        Collections.sort(halls);
        //
        //assign lectures to Halls here :
        // i need to find a way to make the algorithm tell if there is no solution
        // checkAtEachStepTheConstraints
        //

        int iCounter = 0;

        week_timetableont.getLectures().forEach((n) -> System.out.print(n));
        System.out.println();
        System.out.println();

        while (iCounter != 2000){

             int iStudents = 0;
            if(week_timetableont.getLectures().size() != 0) {
                iStudents = week_timetableont.getLectures().get(0).getiNumberOfStudentsAttending();
            }
            else{ iCounter++; continue;}


//                    week_timetableont = recursion(week_timetableont,week_timetableont.getHalls(), week_timetableont.getLectures(),week_timetableont.getAssignedLectures(),
//                                                    week_timetableont.getLectures().get(0).getiNumberOfStudentsAttending() ,week_timetableont.getLectures().get(0).getiNumberOfStudentsAttending(),
//                                        1,0);


            week_timetableont = recursion2(week_timetableont,week_timetableont.getHalls(), week_timetableont.getLectures(),week_timetableont.getAssignedLectures(),
                    week_timetableont.getLectures().get(0).getiNumberOfStudentsAttending() ,week_timetableont.getLectures().get(0).getiNumberOfStudentsAttending(),
                    1,0, 1);

            iCounter++;

            }

        //week_timetableont.getHalls().forEach((n)->System.out.println(n));
        System.out.println("Not Assigned Lectures:");
        System.out.println(week_timetableont.getLectures());

        for(int i = 0; i < week_timetableont.getWeekTimet().size();i++){
            Collections.sort(week_timetableont.getWeekTimet().get(i).getoDslot2());
        }
        week_timetableont.v_print();
            if(week_timetableont.getLectures().size() == 0 ){
                // solution found
                return 1;
            }
            else{
                return 0;
                //opt for running the algorithm again, maybe a bit differently or choose to run another algorithm
            }
        /**/


    }


    /**/



    //END of Class

}
