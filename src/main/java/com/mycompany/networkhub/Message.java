package com.mycompany.networkhub;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

/**
 * Represents a single QuickChat message, along with every piece of
 * validation, hashing, sending and storing logic the brief requires.
 *
 * Each Message object owns its own data (ID, recipient, text, hash).
 * A few pieces of information - the running total of sent messages,
 * and the list of messages that have been sent - are shared across
 * every Message, so they're declared as static fields instead of
 * instance fields.
 *
 * @author Student
 *
 * Reference for the regular expression used in checkRecipientCell():
 * reused and adapted from the cell-phone validation pattern built for
 * the earlier Login task.
 * Oracle (2024) Class Pattern - Java Platform SE Documentation.
 * Available at: https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/regex/Pattern.html
 * (Accessed: 18 October 2026).
 */
public class Message {

    // =========================================================
    //  SHARED STATE - one copy exists for the whole application,
    //  not one copy per Message object.
    // =========================================================

    /**
     * How many messages have been SENT (not stored, not disregarded)
     * since the program started. This also doubles as the "message
     * number" that gets baked into each new message's hash, so the
     * very first message sent is numbered 0, the second is 1, and so on.
     */
    private static int totalMessagesSent = 0;

    /**
     * Keeps a copy of every message that has been successfully sent,
     * in the order it was sent, so printMessages() has something to
     * report on.
     */
    private static final List<Message> sentMessages = new ArrayList<>();

    private static final int MAX_MESSAGE_LENGTH = 250;
    private static final int MAX_ID_LENGTH = 10;

    /**
     * Matches an international dialling code (a plus sign followed by
     * one to three digits) immediately followed by up to ten more
     * digits - the same pattern used to validate cell numbers in the
     * Login task. Example of a match: +27718693002
     */
    private static final Pattern RECIPIENT_PATTERN =
            Pattern.compile("^\\+[0-9]{1,3}[0-9]{1,10}$");

    // =========================================================
    //  PER-MESSAGE STATE - unique to each individual Message object.
    // =========================================================

    private String messageID;    // random 10-digit identifier, kept as a String so leading zeros survive
    private int messageNumber;   // this message's position in the hash (0-indexed, see totalMessagesSent above)
    private String recipient;    // the destination cell number, e.g. "+27718693002"
    private String messageText;  // the body of the message itself
    private String messageHash;  // filled in once createMessageHash() has been called

    /**
     * Normal, everyday constructor. Call this when the user is actually
     * composing a message in the app - it generates a brand-new random
     * message ID for you.
     */
    public Message(String recipient, String messageText) {
        this.recipient = recipient;
        this.messageText = messageText;
        this.messageID = generateRandomMessageID();
        this.messageNumber = totalMessagesSent;
    }

    /**
     * Test-only constructor. A genuinely random message ID can never be
     * predicted, so unit tests that need to check an EXACT expected
     * hash (such as the brief's "00:0:HITONIGHT" example) use this
     * constructor to fix the ID in advance instead.
     *
     * Production code should always use the two-argument constructor
     * above; this one exists purely so LoginTest-style assertEquals
     * tests are reproducible.
     */
    public Message(String recipient, String messageText, String fixedMessageIdForTesting) {
        this.recipient = recipient;
        this.messageText = messageText;
        this.messageID = fixedMessageIdForTesting;
        this.messageNumber = totalMessagesSent;
    }

    /**
     * Builds a random ten-digit numeric string one digit at a time,
     * e.g. "0345981276". Building it digit-by-digit (rather than,
     * say, generating a random int and converting it to a String)
     * guarantees leading zeros are never lost.
     */
    private String generateRandomMessageID() {
        Random random = new Random();
        StringBuilder id = new StringBuilder();
        for (int i = 0; i < MAX_ID_LENGTH; i++) {
            id.append(random.nextInt(10)); // one random digit, 0 through 9
        }
        return id.toString();
    }

    // =========================================================
    //  GETTERS
    // =========================================================

    public String getMessageID() {
        return messageID;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getMessageText() {
        return messageText;
    }

    public String getMessageHash() {
        return messageHash;
    }

    // =========================================================
    //  VALIDATION METHODS
    // =========================================================

    /**
     * Confirms the message ID is no longer than ten characters, as the
     * brief requires. Since generateRandomMessageID() always produces
     * exactly ten digits, this will normally be true - the check
     * matters mainly if an ID was ever set another way (e.g. loaded
     * back in from storage).
     */
    public boolean checkMessageID() {
        return messageID != null && messageID.length() <= MAX_ID_LENGTH;
    }

    /**
     * Checks that the recipient's cell number starts with an
     * international dialling code and isn't too long, then returns the
     * exact success or failure message the brief specifies.
     */
    public String checkRecipientCell() {
        if (recipient != null && RECIPIENT_PATTERN.matcher(recipient).matches()) {
            return "Cell phone number successfully captured.";
        }
        return "Cell phone number is incorrectly formatted or does not contain "
                + "an international code. Please correct the number and try again.";
    }

    /**
     * Checks the message body is no more than 250 characters long.
     * If it's too long, the failure message reports exactly how many
     * characters need to be trimmed, as the brief requires.
     */
    public String checkMessageLength() {
        if (messageText == null) {
            return "Please enter a message of less than 250 characters.";
        }

        int charactersOverLimit = messageText.length() - MAX_MESSAGE_LENGTH;
        if (charactersOverLimit > 0) {
            return "Message exceeds 250 characters by " + charactersOverLimit
                    + "; please reduce the size.";
        }
        return "Message ready to send.";
    }

    // =========================================================
    //  HASH GENERATION
    // =========================================================

    /**
     * Builds the Message Hash the brief requires, in the format:
     *
     *     [first two digits of the message ID] : [this message's number] : [FIRSTLASTWORD]
     *
     * Example: message ID "0012345678", message number 0, and the text
     * "Hi Mike, can you join us for dinner tonight?" produces the hash
     * "00:0:HITONIGHT" - note the first and last words are joined
     * directly together with no separator, in capitals, and with any
     * punctuation (like the "?" after "tonight") stripped off first.
     */
    public String createMessageHash() {
        String idPrefix = messageID.substring(0, 2);

        String[] words = messageText.trim().split("\\s+");
        String firstWord = stripPunctuation(words[0]).toUpperCase();
        String lastWord = stripPunctuation(words[words.length - 1]).toUpperCase();

        messageHash = idPrefix + ":" + messageNumber + ":" + firstWord + lastWord;
        return messageHash;
    }

    /**
     * Strips everything that isn't a letter or a digit from a word,
     * e.g. turns "tonight?" into "tonight". Used so punctuation at the
     * edge of the first/last word doesn't leak into the hash.
     */
    private String stripPunctuation(String word) {
        return word.replaceAll("[^a-zA-Z0-9]", "");
    }

    // =========================================================
    //  SENDING, STORING AND DISREGARDING
    // =========================================================

    /**
     * Carries out whichever choice the user made for this message and
     * returns the matching system message from the brief. This is also
     * where the hash gets generated and the shared send-history/counter
     * get updated - but only for messages that are actually sent.
     *
     * @param choice 1 = Send Message, 2 = Store Message, 3 = Disregard Message
     * @return the message the system should display to the user
     */
    public String sentMessage(int choice) {
        switch (choice) {
            case 1: // Send Message
                createMessageHash();
                sentMessages.add(this);
                totalMessagesSent++;
                return "Message successfully sent.";

            case 2: // Store Message to send later
                storeMessage();
                return "Message successfully stored.";

            case 3: // Disregard Message
                return "Press 0 to delete the message.";

            default:
                return "Invalid option selected; please choose 1, 2 or 3.";
        }
    }

    // =========================================================
    //  REPORTING
    // =========================================================

    /**
     * Returns the full details of every message sent so far this run,
     * one block per message, in the exact field order the brief
     * requires: Message ID, Message Hash, Recipient, Message.
     */
    public static String printMessages() {
        if (sentMessages.isEmpty()) {
            return "No messages have been sent yet.";
        }

        StringBuilder output = new StringBuilder();
        for (Message message : sentMessages) {
            output.append("Message ID: ").append(message.messageID).append("\n")
                  .append("Message Hash: ").append(message.messageHash).append("\n")
                  .append("Recipient: ").append(message.recipient).append("\n")
                  .append("Message: ").append(message.messageText).append("\n")
                  .append("------------------------------\n");
        }
        return output.toString();
    }

    /** Returns how many messages have been SENT (storing/disregarding don't count). */
    public static int returnTotalMessagess() {
        return totalMessagesSent;
    }

    /**
     * Clears the shared counter and send history. This exists purely
     * for unit tests, so each test starts from a clean slate instead of
     * inheriting counts left over by whichever test ran before it.
     */
    public static void resetMessageHistoryForTesting() {
        totalMessagesSent = 0;
        sentMessages.clear();
    }

    // =========================================================
    //  JSON STORAGE
    // =========================================================

    /**
     * Appends this message to storedMessages.json, one JSON object per
     * line, so messages the user chose to "store" survive between runs.
     *
     * A hand-written JSON string is used here (rather than a library
     * like Gson) to keep the project free of extra dependencies for
     * such a small amount of data; a larger application would be
     * better served by a proper JSON library.
     */
    public void storeMessage() {
        String jsonEntry = "{"
                + "\"messageID\":\"" + messageID + "\","
                + "\"recipient\":\"" + recipient + "\","
                + "\"message\":\"" + escapeForJson(messageText) + "\""
                + "}";

        // "true" below means append to the file rather than overwrite it,
        // so previously stored messages aren't lost.
        try (FileWriter writer = new FileWriter("storedMessages.json", true)) {
            writer.write(jsonEntry + System.lineSeparator());
        } catch (IOException e) {
            System.out.println("Could not store the message: " + e.getMessage());
        }
    }

    /** Escapes quotes and backslashes so messageText is safe to embed inside a JSON string. */
    private String escapeForJson(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}