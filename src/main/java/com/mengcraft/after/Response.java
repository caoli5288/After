package com.mengcraft.after;

public class Response {
	public final static String CMD_OKEY = "200 Command okey\r\n";
	public final static String SYS_INFO = "215 UNIX Type: After server by mengcraft.com\r\n";
	public final static String SERVICE_READY = "220 Service ready for new user\r\n";
	public final static String GOOD_BYE = "221 Service closing control connection\r\n";
	public final static String USER_LOGGED = "230 User logged in, proceed\r\n";
	public final static String FILE_ACT_OKEY = "250 Requested file action okay, completed\r\n";
	public final static String USER_OKEY = "331 User name okay, need password\r\n";
	public final static String CMD_ERROR = "500 Syntax error, command unrecognized\r\n";
	public final static String CMD_ARG_ERROR = "501 Syntax error in parameters or arguments\r\n";
	public final static String CMD_NOT_IMPL = "502 Command not implemented\r\n";
	public final static String CMD_ARG_NOT_IMPL = "504 Command not implemented for that parameter\r\n";
	public final static String USER_NOT_LOGGED = "530 Not logged in\r\n";
	public final static String FILE_ACT_ERROR = "550 Requested file action not okay";
}
