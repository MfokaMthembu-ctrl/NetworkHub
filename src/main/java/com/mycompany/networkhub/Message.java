package com.mycompany.networkhub;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

/**
 * Represents a single QuickChat message, together with all of the
 * validation, hashing, sending, storing, searching and reporting logic
 * that the brief requires.
 *
 * Each Message object owns its own data (ID, sender, recipient, text, hash).
 * The five arrays the brief asks for (sent, disregarded and stored messages,
 * message hashes and message IDs) are shared by every Message, so they are
 * declared as static fields instead of instance fields.
 *
 * @author Student
 *
 * References:
 * Oracle (2024) Class Pattern - Java Platform SE Documentation.
 * Available at: https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/regex/Pattern.html
 * (Accessed: 15 November 2026).
 *
 * Google (2024) Gson User Guide - used for writing the stored messages to a
 * JSON file and for reading that file back into an array.
 * Available at: https://github.com/google/gson/blob/main/UserGuide.md
 * (Accessed: 15 November 2026).
 */
public class Message {

    // =========================================================
    //  THE FIVE ARRAYS FROM THE BRIEF (shared by the whole program)
    // =========================================================

    /** Every message that has been SENT, in the order it was sent. */
    private static final List<Message> sentMessages = new ArrayList<>();

    /** Every message the user chose to DISREGARD. */
    private static final List<Message> disregardedMessages = new ArrayList<>();

    /**
     * Every message the user chose to STORE. This array is also what gets
     * written to the JSON file, and it is refilled from that file when the
     * program starts (see loadStoredMessagesFromFile()).
     */
    private static final List<Message> storedMessages = new ArrayList<>();

    /** The hash of every message that has been processed. */
    private static final List<String> messageHashes = new ArrayList<>();

    /** The ID of every message that has been processed. */
    private static final List<String> messageIDs = new ArrayList<>();

    // =========================================================
    //  OTHER SHARED STATE
    // =========================================================

    /**
     * How many messages have been SENT since the program started. This also
     * becomes the "message number" inside each new message's hash, so the
     * first message sent is numbered 0, the second 1, and so on.
     */
    private static int totalMessagesSent = 0;

    /** The name of the logged-in user, recorded as the "sender" of new messages. */
    private static String currentSender = "Unknown sender";

    /** The JSON file that stored messages are saved to and loaded from. */
    private static String storageFile = "storedMessages.json";

    private static final int MAX_MESSAGE_LENGTH = 250;
    private static final int MAX_ID_LENGTH = 10;

    /**
     * Matches an international dialling code (a plus sign followed by one to
     * three digits) immediately followed by up to ten more digits - the same
     * pattern used to validate cell numbers in the Login task.
     * Example of a match: +27718693002
     */
    private static final Pattern RECIPIENT_PATTERN =
            Pattern.compile("^\\+[0-9]{1,3}[0-9]{1,10}$");

    // =========================================================
    //  PER-MESSAGE STATE - unique to each individual Message object
    // =========================================================

    private String messageID;    // ten-digit identifier, kept as a String so leading zeros survive
    private int messageNumber;   // this message's position in the hash (0-indexed)
    private String sender;       // who sent the message (the logged-in user)
    private String recipient;    // the destination cell number, e.g. "+27718693002"
    private String messageText;  // the body of the message itself
    private String messageHash;  // filled in once createMessageHash() has been called

    /**
     * Normal, everyday constructor. Call this when the user is composing a
     * message in the app - it generates a brand-new random message ID.
     */
    public Message(String recipient, String messageText) {
        this.sender = currentSender;
        this.recipient = recipient;
        this.messageText = messageText;
        this.messageID = generateRandomMessageID();
        this.messageNumber = totalMessagesSent;
    }

    /**
     * Constructor that lets the caller choose the message ID. A random ID can
     * never be predicted, so unit tests that need an EXACT expected value
     * (such as the "00:0:HITONIGHT" hash, or the developer-defined ID of
     * Test Data Message 4) use this constructor to fix the ID in advance.
     */
    public Message(String recipient, String messageText, String fixedMessageId) {
        this.sender = currentSender;
        this.recipient = recipient;
        this.messageText = messageText;
        this.messageID = fixedMessageId;
        this.messageNumber = totalMessagesSent;
    }

    /**
     * Private constructor used only when a stored message is read back in
     * from the JSON file: every value, including the hash, was saved earlier.
     */
    private Message(String sender, String recipient, String messageText,
                    String messageID, String messageHash) {
        this.sender = (sender == null) ? "Unknown sender" : sender;
        this.recipient = recipient;
        this.messageText = (messageText == null) ? "" : messageText;
        this.messageID = messageID;
        this.messageHash = messageHash;
        this.messageNumber = 0;
    }

    /**
     * Builds a random ten-digit numeric string one digit at a time, e.g.
     * "0345981276". Building it digit-by-digit (rather than generating a
     * random int and converting it) guarantees leading zeros are never lost.
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
    //  GETTERS AND SETTINGS
    // =========================================================

    public String getMessageID() {
        return messageID;
    }

    public String getSender() {
        return sender;
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

    /** Records who is logged in, so new messages know their sender. */
    public static void setCurrentSender(String senderName) {
        currentSender = senderName;
    }

    /** Changes which JSON file stored messages are saved to (used by the tests). */
    public static void setStorageFile(String filePath) {
        storageFile = filePath;
    }

    // =========================================================
    //  VALIDATION METHODS
    // =========================================================

    /**
     * Confirms the message ID is no longer than ten characters, as the
     * brief requires.
     */
    public boolean checkMessageID() {
        return messageID != null && messageID.length() <= MAX_ID_LENGTH;
    }

    /**
     * Returns the confirmation text the brief requires once a message ID
     * has been created: "Message ID generated: <Message ID>".
     */
    public String returnMessageIDMessage() {
        return "Message ID generated: " + messageID;
    }

    /**
     * Checks that the recipient's cell number starts with an international
     * dialling code and isn't too long, then returns the exact success or
     * failure message the brief specifies.
     */
    public String checkRecipientCell() {
        if (recipient != null && RECIPIENT_PATTERN.matcher(recipient).matches()) {
            return "Cell phone number successfully captured.";
        }
        return "Cell phone number is incorrectly formatted or does not contain "
                + "an international code. Please correct the number and try again.";
    }

    /**
     * Checks the message body is no more than 250 characters long. If it's
     * too long, the failure message reports exactly how many characters need
     * to be trimmed, as the brief requires.
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
     * Builds the Message Hash in the format:
     *
     *     [first two digits of the message ID] : [message number] : [FIRSTLASTWORD]
     *
     * Example: ID "0012345678", message number 0 and the text
     * "Hi Mike, can you join us for dinner tonight?" gives "00:0:HITONIGHT".
     * The first and last words are joined with no separator, in capitals,
     * with punctuation (like the "?" after "tonight") stripped off first.
     */
    public String createMessageHash() {
        String idPrefix = messageID.substring(0, 2);

        String[] words = messageText.trim().split("\\s+");
        String firstWord = stripPunctuation(words[0]).toUpperCase();
        String lastWord = stripPunctuation(words[words.length - 1]).toUpperCase();

        messageHash = idPrefix + ":" + messageNumber + ":" + firstWord + lastWord;
        return messageHash;
    }

    /** Removes everything that isn't a letter or digit, e.g. "tonight?" becomes "tonight". */
    private String stripPunctuation(String word) {
        return word.replaceAll("[^a-zA-Z0-9]", "");
    }

    // =========================================================
    //  SENDING, DISREGARDING AND STORING
    // =========================================================

    /**
     * Carries out whichever choice the user made for this message, files the
     * message into the correct array and returns the matching system message.
     *
     * @param choice 1 = Send Message, 2 = Disregard Message, 3 = Store Message
     *               (the same order the brief lists the options in)
     * @return the message the system should display to the user
     */
    public String sentMessage(int choice) {
        switch (choice) {
            case 1: // Send Message
                recordHashAndID();
                sentMessages.add(this);
                totalMessagesSent++;
                return "Message successfully sent.";

            case 2: // Disregard Message
                recordHashAndID();
                disregardedMessages.add(this);
                return "Press 0 to delete the message.";

            case 3: // Store Message to send later
                recordHashAndID();
                storeMessage();
                return "Message successfully stored.";

            default:
                return "Invalid option selected; please choose 1, 2 or 3.";
        }
    }

    /**
     * Creates this message's hash and adds both the hash and the message ID
     * to their arrays. Called once for every message that is processed.
     */
    private void recordHashAndID() {
        createMessageHash();
        messageHashes.add(messageHash);
        messageIDs.add(messageID);
    }

    /**
     * Adds this message to the Stored Messages array and then saves the whole
     * array to the JSON file, so stored messages survive between runs.
     */
    public void storeMessage() {
        storedMessages.add(this);
        saveStoredMessagesToFile();
    }

    // =========================================================
    //  READING THE ARRAYS (copies, so callers cannot change the originals)
    // =========================================================

    /** The text of every sent message. */
    public static List<String> getSentMessages() {
        return textsOf(sentMessages);
    }

    /** The text of every disregarded message. */
    public static List<String> getDisregardedMessages() {
        return textsOf(disregardedMessages);
    }

    /** The text of every stored message. */
    public static List<String> getStoredMessages() {
        return textsOf(storedMessages);
    }

    /** Every message hash that has been created. */
    public static List<String> getMessageHashes() {
        return new ArrayList<>(messageHashes);
    }

    /** Every message ID that has been created. */
    public static List<String> getMessageIDs() {
        return new ArrayList<>(messageIDs);
    }

    private static List<String> textsOf(List<Message> messages) {
        List<String> texts = new ArrayList<>();
        for (Message message : messages) {
            texts.add(message.messageText);
        }
        return texts;
    }

    // =========================================================
    //  STORED MESSAGES MENU FEATURES (2a - 2f in the brief)
    // =========================================================

    /** 2a: lists the sender and recipient of every stored message. */
    public static String displaySenderAndRecipientOfStoredMessages() {
        if (storedMessages.isEmpty()) {
            return "There are no stored messages.";
        }

        List<String> lines = new ArrayList<>();
        for (Message message : storedMessages) {
            lines.add("Sender: " + message.sender
                    + " | Recipient: " + message.recipient);
        }
        return String.join("\n", lines);
    }

    /** 2b: returns the text of the longest stored message. */
    public static String getLongestStoredMessage() {
        Message longest = longestIn(storedMessages);
        return (longest == null) ? "There are no stored messages." : longest.messageText;
    }

    /**
     * Returns the text of the longest message across ALL the arrays (sent,
     * disregarded and stored) - used for the "Display the longest Message"
     * unit test, which looks at Test Data Messages 1 to 4.
     */
    public static String getLongestMessage() {
        List<Message> everyMessage = new ArrayList<>();
        everyMessage.addAll(sentMessages);
        everyMessage.addAll(disregardedMessages);
        everyMessage.addAll(storedMessages);

        Message longest = longestIn(everyMessage);
        return (longest == null) ? "There are no messages." : longest.messageText;
    }

    /** Walks through a list and keeps track of the message with the most characters. */
    private static Message longestIn(List<Message> messages) {
        Message longest = null;
        for (Message message : messages) {
            if (longest == null
                    || message.messageText.length() > longest.messageText.length()) {
                longest = message;
            }
        }
        return longest;
    }

    /**
     * 2c: finds a sent or stored message by its ID.
     *
     * @return the matching message, or null if no message has that ID
     */
    public static Message findMessageByID(String id) {
        if (id == null) {
            return null;
        }
        String wantedId = id.trim();

        for (Message message : sentMessages) {
            if (wantedId.equals(message.messageID)) {
                return message;
            }
        }
        for (Message message : storedMessages) {
            if (wantedId.equals(message.messageID)) {
                return message;
            }
        }
        return null;
    }

    /**
     * 2d: returns the text of every sent or stored message addressed to the
     * given recipient, in the order they were added.
     */
    public static List<String> searchMessagesByRecipient(String recipientNumber) {
        List<String> matches = new ArrayList<>();
        if (recipientNumber == null) {
            return matches;
        }
        String wantedRecipient = recipientNumber.trim();

        for (Message message : sentMessages) {
            if (wantedRecipient.equals(message.recipient)) {
                matches.add(message.messageText);
            }
        }
        for (Message message : storedMessages) {
            if (wantedRecipient.equals(message.recipient)) {
                matches.add(message.messageText);
            }
        }
        return matches;
    }

    /**
     * 2e: deletes a stored message using its hash. The message is removed
     * from the Stored Messages array, from the hash and ID arrays, and from
     * the JSON file.
     *
     * @return the confirmation (or "not found") message to show the user
     */
    public static String deleteMessageByHash(String hash) {
        if (hash == null) {
            return "No stored message was found with that hash.";
        }
        String wantedHash = hash.trim();

        for (int i = 0; i < storedMessages.size(); i++) {
            Message message = storedMessages.get(i);

            if (wantedHash.equalsIgnoreCase(message.messageHash)) {
                storedMessages.remove(i);
                messageHashes.remove(message.messageHash);
                messageIDs.remove(message.messageID);
                saveStoredMessagesToFile();

                return "Message: \"" + withoutFinalFullStop(message.messageText)
                        + "\" successfully deleted.";
            }
        }
        return "No stored message was found with the hash: " + wantedHash;
    }

    /**
     * The brief's example output shows the deleted sentence WITHOUT its final
     * full stop inside the quotation marks, so one trailing "." is dropped.
     */
    private static String withoutFinalFullStop(String text) {
        if (text.endsWith(".")) {
            return text.substring(0, text.length() - 1);
        }
        return text;
    }

    /** 2f: a report with the full details of every stored message. */
    public static String displayStoredMessagesReport() {
        if (storedMessages.isEmpty()) {
            return "There are no stored messages.";
        }

        StringBuilder report = new StringBuilder();
        for (Message message : storedMessages) {
            report.append("Message ID: ").append(message.messageID).append("\n")
                  .append("Message Hash: ").append(message.messageHash).append("\n")
                  .append("Sender: ").append(message.sender).append("\n")
                  .append("Recipient: ").append(message.recipient).append("\n")
                  .append("Message: ").append(message.messageText).append("\n")
                  .append("------------------------------\n");
        }
        return report.toString();
    }

    // =========================================================
    //  REPORTING ON SENT MESSAGES
    // =========================================================

    /**
     * Returns the full details of every message sent so far this run, in the
     * order the brief requires: Message ID, Message Hash, Recipient, Message.
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

    // =========================================================
    //  JSON STORAGE (using the Gson library)
    // =========================================================

    /**
     * A simple "plain data" copy of a stored message. Gson turns objects of
     * this class into JSON (and back again) using the field names below.
     */
    private static class StoredMessageRecord {
        private String messageID;
        private String messageHash;
        private String sender;
        private String recipient;
        private String message;

        // Gson needs a constructor with no parameters to rebuild objects.
        StoredMessageRecord() {
        }

        StoredMessageRecord(Message source) {
            this.messageID = source.messageID;
            this.messageHash = source.messageHash;
            this.sender = source.sender;
            this.recipient = source.recipient;
            this.message = source.messageText;
        }
    }

    /**
     * Writes the whole Stored Messages array to the JSON file, replacing
     * whatever was in the file before. The result is a JSON array, e.g.
     * [ { "messageID": "...", "recipient": "...", "message": "..." }, ... ]
     */
    private static void saveStoredMessagesToFile() {
        List<StoredMessageRecord> records = new ArrayList<>();
        for (Message message : storedMessages) {
            records.add(new StoredMessageRecord(message));
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try (Writer writer = Files.newBufferedWriter(Paths.get(storageFile),
                StandardCharsets.UTF_8)) {
            gson.toJson(records, writer);
        } catch (IOException e) {
            System.out.println("Could not store the message: " + e.getMessage());
        }
    }

    /**
     * Reads the JSON file back into the Stored Messages array (and the hash
     * and ID arrays). Call this once when the program starts. If the file
     * does not exist yet, nothing is loaded.
     *
     * @return how many stored messages were loaded
     */
    public static int loadStoredMessagesFromFile() {
        // Forget any stored messages already in memory so nothing is
        // loaded twice if this method is called more than once.
        for (Message existing : storedMessages) {
            messageHashes.remove(existing.messageHash);
            messageIDs.remove(existing.messageID);
        }
        storedMessages.clear();

        Path path = Paths.get(storageFile);
        if (!Files.exists(path)) {
            return 0;
        }

        // TypeToken tells Gson we want a LIST of StoredMessageRecord objects.
        Type listType = new TypeToken<List<StoredMessageRecord>>() { }.getType();

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            List<StoredMessageRecord> records = new Gson().fromJson(reader, listType);

            if (records != null) {
                for (StoredMessageRecord record : records) {
                    if (record == null) {
                        continue;
                    }
                    Message loaded = new Message(record.sender, record.recipient,
                            record.message, record.messageID, record.messageHash);

                    storedMessages.add(loaded);
                    messageHashes.add(loaded.messageHash);
                    messageIDs.add(loaded.messageID);
                }
            }
        } catch (IOException | JsonParseException e) {
            System.out.println("Could not read the stored messages file: " + e.getMessage());
        }

        return storedMessages.size();
    }

    // =========================================================
    //  TEST SUPPORT
    // =========================================================

    /**
     * Empties every shared array and resets the counter, so each unit test
     * starts from a clean slate instead of inheriting leftovers from
     * whichever test ran before it. The JSON file itself is NOT touched.
     */
    public static void resetMessageHistoryForTesting() {
        totalMessagesSent = 0;
        sentMessages.clear();
        disregardedMessages.clear();
        storedMessages.clear();
        messageHashes.clear();
        messageIDs.clear();
    }
}