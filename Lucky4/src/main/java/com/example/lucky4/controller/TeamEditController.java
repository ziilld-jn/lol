package com.example.lucky4.controller;

import com.example.lucky4.model.Member;
import com.example.lucky4.model.MemberUpdate;
import com.example.lucky4.service.MemberStore;
import com.example.lucky4.service.PhotoStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Optional;

/** Endpoints used by /edit.html to change member details and photos. */
@RestController
@RequestMapping("/api/edit")
public class TeamEditController {

    private static final String CODE_HEADER = "X-Edit-Code";

    private final MemberStore store;
    private final PhotoStorage photos;
    private final String editCode;

    public TeamEditController(MemberStore store,
                              PhotoStorage photos,
                              @Value("${lucky4.edit-code:}") String editCode) {
        this.store = store;
        this.photos = photos;
        this.editCode = editCode;
    }

    /** The edit page calls this to test a code. */
    @GetMapping("/check")
    public ResponseEntity<?> check(@RequestHeader(value = CODE_HEADER, required = false) String code) {
        if (!codeOk(code)) {
            return error(HttpStatus.UNAUTHORIZED, "Wrong edit code.");
        }
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PutMapping("/members/{id}")
    public ResponseEntity<?> update(@PathVariable("id") int id,
                                    @RequestHeader(value = CODE_HEADER, required = false) String code,
                                    @RequestBody MemberUpdate body) {
        if (!codeOk(code)) {
            return error(HttpStatus.UNAUTHORIZED, "Wrong edit code.");
        }
        if (store.find(id).isEmpty()) {
            return error(HttpStatus.NOT_FOUND, "That member does not exist.");
        }
        try {
            return ResponseEntity.ok(store.update(id, body));
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping(value = "/members/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadPhoto(@PathVariable("id") int id,
                                         @RequestHeader(value = CODE_HEADER, required = false) String code,
                                         @RequestParam("photo") MultipartFile photo) {
        if (!codeOk(code)) {
            return error(HttpStatus.UNAUTHORIZED, "Wrong edit code.");
        }
        Optional<Member> current = store.find(id);
        if (current.isEmpty()) {
            return error(HttpStatus.NOT_FOUND, "That member does not exist.");
        }
        try {
            String newPath = photos.save(id, photo);
            Member updated = store.setImage(id, newPath);
            photos.deleteQuietly(current.get().image());
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IOException e) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save the photo.");
        }
    }

    /** If lucky4.edit-code is blank, no code is required. */
    private boolean codeOk(String provided) {
        if (editCode == null || editCode.isBlank()) {
            return true;
        }
        byte[] expected = editCode.getBytes(StandardCharsets.UTF_8);
        byte[] actual = (provided == null ? "" : provided).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }

    private static ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}