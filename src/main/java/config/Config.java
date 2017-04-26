package config;

public class Config {
	public int port = 6881;
	public int torPort = 9050;
	public String downloadDir = "downloads/";
	public boolean useTor = true;
	public String torBinary = "/home/till/bin/tor";
	public int orPort = 5000;
	public String[] dirs = {
	    "DirAuthority test000a orport=5000 no-v2 v3ident=3857EB8A82FE4BE507D68D679ACA2903FA8DFB75 10.1.136.65:7000 9B1536DAE50167A3998BF8C535CBC1920069E41A",
	    "DirAuthority test001a orport=5001 no-v2 v3ident=5EC663C203452C380033AD16CA789B56D6AF6B72 10.1.136.65:7001 F65CCD34ED215F108FD1833EF0A8E11373619CAF",
        "DirAuthority test002a orport=5002 no-v2 v3ident=0EAA7EC3AAA20AD1553DD7737CCEE5F80011688F 10.1.136.65:7002 D3A47DBC92B04CEFF857B45F424BF36134968301"
	};
	public boolean useDht = true;
	public int dhtPort = 5100;
	public String dhtBootstrapAddress = "127.0.0.1";
	public int dhtBootstrapPort = 5100;
}
