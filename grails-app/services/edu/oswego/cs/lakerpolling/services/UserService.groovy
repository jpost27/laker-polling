package edu.oswego.cs.lakerpolling.services

import edu.oswego.cs.lakerpolling.domains.AuthToken
import edu.oswego.cs.lakerpolling.domains.User
import edu.oswego.cs.lakerpolling.util.Pair
import edu.oswego.cs.lakerpolling.util.RoleType
import grails.transaction.Transactional

@Transactional
class UserService {

    /**
     * Checks if the given user is an Instructor
     * @param user - The user to check
     * @return A boolean representing whether the user is an Instructor
     */
    boolean checkIfInstructor(User user) { user.role.type.equals(RoleType.INSTRUCTOR) }

    /**
     * Gets the user with the specified access token
     * @param token - the access token for the user
     * @return The user associated with access token
     */
    Optional<User> getUser(String token) {
        AuthToken authToken = AuthToken.findByAccessToken(token)
        User user = authToken != null ? User.findByAuthToken(authToken) : null
        return user != null ? Optional.of(user) : Optional.empty()
    }

    /**
     * Gets the user associated with the given email or, if no such user exists, creates a placeholder user account for
     * that email. This placeholder account will allow instructors to add students to courses before the students have
     * registered with the system.
     * @param email - The email of the user
     * @return A User object
     */
    User getOrMakeByEmail(String email) {
        User user

        user = User.findByEmail(email)

        if (user == null) {
            user = new User(email: email)
            user.save(flush: true, failOnError: true)
        }

        return user
    }

    /**
     * Method to get, update or insert a user from the given payload.
     * @param payload - The payload to grab data from.
     * @return A pair containing a User and their associated AuthToken should all operations go successfully.
     */
    Optional<Pair<User, AuthToken>> getMakeOrUpdate(String subj, String first, String last, String imageUrl,
                                                    String accessTokenHash, String email) {
        User user
        AuthToken token

        token = AuthToken.findBySubject(subj)

        // We have a token for this user.
        if (token != null) {

            user = token.user
            if (user.email != email) {
                user.email = email
            }

            if (token.accessToken != accessTokenHash) {
                token.accessToken = accessTokenHash
            }

            user.save(flush: true, failOnError: true)
            token.save(flush: true, failOnError: true)

        } else {
            // this may be a pre-loaded account or this is a new user.

            //attempt to find them in the db by email
            user = User.findByEmail(email)

            if (user == null) {// didn't find it, so this is a new user.
                user = new User(firstName: first, lastName: last, imageUrl: imageUrl, email: email).save(flush: true, failOnError: true)
            } else {//found the user by email, this must be a pre-loaded account
                if (user.imageUrl == null) {
                    user.imageUrl = imageUrl
                }

                if (user.firstName == null) {
                    user.firstName = first
                }

                if (user.lastName == null) {
                    user.lastName = last
                }

                user.save(flush: true, failOnError: true)
            }

            //associate a token with the user
            token = new AuthToken(user: user, subject: subj, accessToken: accessTokenHash).save(flush: true, failOnError: true)
        }

        user != null ? Optional.of(new Pair<User, AuthToken>(user, token))
                : Optional.empty()

    }

}
