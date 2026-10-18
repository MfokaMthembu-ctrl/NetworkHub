package com.mycompany.networkhub;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Message class, using the exact test data supplied
 * in the brief (Test Case 1: recipient +27718693002, message about
 * dinner tonight).
 *
 * @author Student
 */
public class MessageTest {

    /**
     * Runs before every single test method. Since totalMessagesSent and
     * sentMessages are shared (static) across all Message objects, we
     * reset them before each test so earlier tests can't leak counts
     * into this one and throw assertions off.
     */
    @BeforeEach
    public void resetSharedState() {
        Message.resetMessageHistoryForTesting();
    }

    // ---------- Message length tests ----------

    @Test
    public void testMessageUnderLimitReturnsReadyToSend() {
        Message message = new Message("+27718693002",
                "Hi Mike, can you join us for dinner tonight?");
        assertEquals("Message ready to send.", message.checkMessageLength());
    }

    @Test
    public void testMessageOverLimitReturnsExceedsMessage() {
        // Build a message text that is deliberately 260 characters long,
        // i.e. 10 characters over the 250 limit.
        String longText = "A".repeat(260);
        Message message = new Message("+27718693002", longText);
        assertEquals("Message exceeds 250 characters by 10; please reduce the size.",
                message.checkMessageLength());
    }

    // ---------- Recipient cell number tests ----------

    @Test
    public void testRecipientCorrectlyFormatted() {
        Message message = new Message("+27718693002",
                "Hi Mike, can you join us for dinner tonight?");
        assertEquals("Cell phone number successfully captured.",
                message.checkRecipientCell());
    }

    @Test
    public void testRecipientIncorrectlyFormatted() {
        Message message = new Message("08575975889",
                "Hi Keegan, did you receive the payment?");
        assertEquals("Cell phone number is incorrectly formatted or does not contain "
                + "an international code. Please correct the number and try again.",
                message.checkRecipientCell());
    }

    // ---------- Message hash tests ----------

    @Test
    public void testMessageHashMatchesTestCase1() {
        // The fixed-ID constructor is used here so the first two digits
        // of the hash are predictable ("00"), matching the brief's
        // worked example of "00:0:HITONIGHT" for Test Case 1.
        Message message = new Message("+27718693002",
                "Hi Mike, can you join us for dinner tonight?", "0012345678");

        assertEquals("00:0:HITONIGHT", message.createMessageHash());
    }

    @Test
    public void testMessageHashIncrementsWithEachSentMessage() {
        Message first = new Message("+27718693002", "Hi Mike, dinner tonight?", "0011111111");
        first.sentMessage(1); // sending increments totalMessagesSent

        Message second = new Message("+27718693002", "Hi Keegan, payment received", "0022222222");
        String secondHash = second.createMessageHash();

        // The second message sent should carry message number 1, not 0.
        assertTrue(secondHash.contains(":1:"));
    }

    // ---------- Sending, storing, disregarding ----------

    @Test
    public void testSendMessageReturnsSuccessMessage() {
        Message message = new Message("+27718693002",
                "Hi Mike, can you join us for dinner tonight?");
        assertEquals("Message successfully sent.", message.sentMessage(1));
    }

    @Test
    public void testDisregardMessageReturnsDeletePrompt() {
        Message message = new Message("08575975889",
                "Hi Keegan, did you receive the payment?");
        assertEquals("Press 0 to delete the message.", message.sentMessage(3));
    }

    // ---------- Total messages sent ----------

    @Test
    public void testReturnTotalMessagesCountsOnlySentMessages() {
        Message sent = new Message("+27718693002", "Hi Mike, dinner tonight?");
        sent.sentMessage(1); // counts

        Message disregarded = new Message("+27718693002", "Never mind");
        disregarded.sentMessage(3); // does not count

        assertEquals(1, Message.returnTotalMessagess());
    }
}
