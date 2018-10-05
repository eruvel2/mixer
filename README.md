mixer is a coin mixer simulator

To build this project use "gradlew clean build"

Run the executable from the command line com.mixer.JobMixer [house acct]. If not specified the default house acct is house5.

Program logic:

1. Start and create TransActor
2. TransActor reads all the transactions and keeps track of payments made to and from house acct
3. If there any unpaid amounts outstanding, payments are made
4. The last processed transaction timestamp is kept for the next scheduled read
5. TransActor will reschedule reading the transactions and start processing from the last processed transaction
6. Payments made by TransActor to a given account are split into 2 random amounts

Limitations:

1. As there are no API's to read transactions starting from a given timestamp all transactions are read each scheduled time.
The code keeps track of the last processed timestamp so there are no duplicate payments made. However all records must still be read
2. For simplicity, no facility was provided to store the last process timestamp. Therefore on restart, transactions are processed from
the beginning. However, the code calculates payments already made so there are no duplicate payments.
3. The json had to be fudged so it could be read into a case class. An object id was prefixed to the json.

Possible Improvements:

1. Don't create a mega object with the json records, rather process the JSON AST directly
2. If there was a failure in sending the payments the last timestamp process may not be accurate and payments would not be made until a restart.
3. We may want to schedule payments at some random time in the future to make more anonymous the source of the funds
4. I used one house address but this could easily be extended to use multiple house addresses
5. I did not charge a fee! I am a nice guy, but the logic for fee adding can be implemented easily