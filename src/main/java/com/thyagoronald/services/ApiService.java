package com.thyagoronald.services;

import java.io.PrintWriter;

public interface ApiService {
    void handleQuery(String input, PrintWriter out);
}
