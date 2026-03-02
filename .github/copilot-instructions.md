This repository (Nebula) is the single project for all parts of the Nebula language. nebc/ is the compiler, written in Java. std/ is the standard library, written in Nebula itself, it has std/runtime/ which adds the minimum necessary to be able to compile natively into all platform (current development only targets linux for faster development) without libc dependency so the Nebula language is completely agnostic from the platform. 

Write modular, production-grade, escalable code, prioritizing well estructured solutions rather than quick hacky fixes / features. E.g.: structure repeating patterns / code into helper methods.

Use the Allman style for all languages, e.g.: Java, Nebula, etc.

Every time, before coding, read thoroughly these files ./cvt.md (Which defines the Nebula language core philosophy and paradigm) and ./spec/revised/full.neb which is a example of all of the Nebula language features, both semantically and syntactically. So you completely understand Neubla's intended direction and scope.

When recompiling the nebc compiler use `compilenebc` a fish alias made for convenience:
function compilenebc
    cd /home/juanpa/dev/nebula/nebc/
    /bin/sh /home/juanpa/apps/idea/plugins/maven/lib/maven3/bin/mvn -Didea.version=2025.2.6 -Dmaven.ext.class.path=/home/juanpa/apps/idea/plugins/maven/lib/maven-event-listener.jar -Djansi.passthrough=true -Dstyle.color=always -Dmaven.repo.local=/home/juanpa/.m2/repository package -f pom.xml
end

If you need to use maven, use this path:/home/juanpa/apps/idea/plugins/maven/lib/maven3/bin/mvnas maven isnt globally installed

To compile the standard library (std/) with nebc, use these flags:
◄ nebula   0s ◎
nebnebc --library std/ -o neb --nostdlib     
to ensure that it doesnt try to link with itself, which will cause redefinition errors.

Finally, acknowledge that your gonna be using fish, so you have to live up to its quirks, mainly syntactical differences from regular sh / bash and the use of $status instead of $0. If needed use /bin/bash everytime, e.g.: prepending every command with it