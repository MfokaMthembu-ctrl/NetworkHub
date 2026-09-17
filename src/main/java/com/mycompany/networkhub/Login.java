/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.networkhub;
import java.util.regex.Pattern;

/**
 *
 * @author Student
 */

public class Login {

    ///attributes
    private String firstName;
    private String lastName;
    private String username;
    private String password;
    private String cellPhoneNumber;

    /**
     * Matches an international dialling code (+ followed by 1 to 3 digits)
     * immediately followed by a number that is no more than ten digits long.
     * Example of a valid number: +27838968976
     */
    private static final Pattern CELL_PHONE_PATTERN =
            Pattern.compile("^\\+[0-9]{1,3}[0-9]{1,10}$");

    public Login(String firstName, String lastName, String username,
                 String password, String cellPhoneNumber) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.password = password;
        this.cellPhoneNumber = cellPhoneNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getCellPhoneNumber() {
        return cellPhoneNumber;
    }

    /**
     * Ensures the username contains an underscore and is no more than
     * five characters long.
     */
    public boolean checkUserName() {
        return username != null
                && username.contains("_")
                && username.length() <= 5;
    }

    /**
     * Ensures the password is at least eight characters long and contains
     * a capital letter, a number and a special character.
     */
    public boolean checkPasswordComplexity() {
        if (password == null || password.length() < 8) {
            return false;
        }

        boolean hasCapital = false;
        boolean hasNumber = false;
        boolean hasSpecial = false;

        for (char character : password.toCharArray()) {
            if (Character.isUpperCase(character)) {
                hasCapital = true;
            } else if (Character.isDigit(character)) {
                hasNumber = true;
            } else if (!Character.isLetterOrDigit(character)
                    && !Character.isWhitespace(character)) {
                hasSpecial = true;
            }
        }

        return hasCapital && hasNumber && hasSpecial;
    }

    /**
     * Ensures the cell phone number is the correct length and contains
     * the international country code.
     */
    public boolean checkCellPhoneNumber() {
        return cellPhoneNumber != null
                && CELL_PHONE_PATTERN.matcher(cellPhoneNumber).matches();
    }

    /**
     * Individual capture message for the username.
     */
    public String returnUsernameMessage() {
        if (checkUserName()) {
            return "Username successfully captured.";
        }
        return "Username is not correctly formatted; please ensure that your "
                + "username contains an underscore and is no more than five "
                + "characters in length.";
    }

    /**
     * Individual capture message for the password.
     */
    public String returnPasswordMessage() {
        if (checkPasswordComplexity()) {
            return "Password successfully captured.";
        }
        return "Password is not correctly formatted; please ensure that the "
                + "password contains at least eight characters, a capital "
                + "letter, a number, and a special character.";
    }

    /**
     * Individual capture message for the cell phone number.
     */
    public String returnCellPhoneMessage() {
        if (checkCellPhoneNumber()) {
            return "Cell number successfully captured.";
        }
        return "Cell number is incorrectly formatted or does not contain an "
                + "international code; please correct the number and try again.";
    }

    /**
     * Returns the overall registration message, checking each condition in turn.
     */
    public String registerUser() {
        if (!checkUserName()) {
            return returnUsernameMessage();
        }
        if (!checkPasswordComplexity()) {
            return returnPasswordMessage();
        }
        if (!checkCellPhoneNumber()) {
            return returnCellPhoneMessage();
        }
        return "Welcome " + firstName + ", " + lastName
                + " it is great to see you.";
    }

    /**
     * Verifies that the login details entered match the details stored
     * when the user registered.
     */
    public boolean loginUser(String enteredUsername, String enteredPassword) {
        return username != null
                && password != null
                && username.equals(enteredUsername)
                && password.equals(enteredPassword);
    }

    /**
     * Returns the messaging for a successful or a failed login.
     */
    public String returnLoginStatus(boolean loginSuccessful) {
        if (loginSuccessful) {
            return "Welcome " + firstName + ", " + lastName
                    + " it is great to see you again.";
        }
        
    }
}
