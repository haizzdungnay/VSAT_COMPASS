package com.vsatcompass.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudioPlayResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String audioKey;
    private int playCount;
    private int maxPlays;
    private boolean allowed;
}
