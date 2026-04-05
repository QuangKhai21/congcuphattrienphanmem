package com.db7.j2ee_quanlythucung.util;

import org.springframework.stereotype.Component;

@Component("avatarUrls")
public class AvatarUrlFormatter {

    public String normalize(String url) {
        return AvatarUrlUtils.normalize(url);
    }

    public String nameInitial(String fullName) {
        return AvatarUrlUtils.nameInitial(fullName);
    }
}
