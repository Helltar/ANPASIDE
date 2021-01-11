package com.github.helltar.anpaside.editor;

import java.util.regex.Pattern;

public class Patterns {

    public static final Pattern stringsPattern = Pattern.compile("'(.*?)'");
    public static final Pattern numbersPattern = Pattern.compile("(\\b(\\d*[.]?\\d+)\\b)");
    public static final Pattern commentsPattern = Pattern.compile("/\\*(?:.|[\\n\\r])*?\\*/|(?<!:)//.*|#.*");
    public static final Pattern keywordsPattern = Pattern.compile("(?<=\\b)((begin)|(end)|(program)|(var)|(function)|(procedure)|(if)|(for))(?=\\b)", Pattern.CASE_INSENSITIVE);
}
