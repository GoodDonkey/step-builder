package com.donkey.stepbuilder.test;


import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;

@TestAnnotation
@AllArgsConstructor
@NoArgsConstructor
@Setter(AccessLevel.PACKAGE)
public class SSome {
    private String propOne;
    private String propTwo;
    private String propThree;
    private String propFour;
    
    public SSome(String propOne) {
        this.propOne = propOne;
    }
    
    @Override
    public String
    toString() {
        return "{\"SSome\":{" + "\"propOne\":" + ((propOne != null) ? ("\"" + propOne + "\"") : null) + "}}";
    }
}
