
perl util\mkfiles.pl >MINFO
perl util\mk1mf.pl debug no-asm VC-NT >ms\nt-debug.mak
perl util\mk1mf.pl debug dll no-asm VC-NT >ms\ntdll-debug.mak

perl util\mkdef.pl libeay NT > ms\libeay32.def
perl util\mkdef.pl ssleay NT > ms\ssleay32.def
