// /team_berp/src/main/java/com/team/berp/domain/ItemType.java

package com.team.berp.domain;

public enum ItemType {
	raw, //자재
	product; // 완제품
	
    public String getPrefix() {
        return switch (this) {
            case raw -> "MAT";
            case product -> "PRD";
           
        };
    }
}
