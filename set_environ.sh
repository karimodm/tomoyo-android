#!/bin/bash
# Copyright 2013 Andrea Villa <andreakarimodm@gmail.com>

export ANDRO_BASE=/mnt/data/android_build
export PATH=$ANDRO_BASE/bin:$PATH
export USE_CCACHE=1
export CCACHE_DIR=$ANDRO_BASE/ccache
source work/build/envsetup.sh
work/prebuilts/misc/linux-x86/ccache/ccache -M 50G
cd work
  lunch full-eng
  ln -sfn ../prebuilt out/
cd ..
export ANDROID="$ANDRO_BASE/work"
export IMG="$ANDROID/out/target/product/generic"
export GOLDFISH="$ANDRO_BASE/goldfish"

# For convenience...
alias ANDRO='cd $ANDRO_BASE'
alias W='cd $ANDRO_BASE/work'
alias TARG='cd $ANDRO_BASE/work/out/target/product/generic'
alias makewout='make -j 4 showcommands 2>&1 | tee ../FULL_OUT_debug'
alias make4d='make -j 4 showcommands' 
# Before compiling just use the right conf:
# kmake goldfish_armv7_defconfig
# sed -e 's/# CONFIG_CCSECURITY_OMIT_USERSPACE_LOADER is not set/CONFIG_CCSECURITY_OMIT_USERSPACE_LOADER=y/' -- config.ccs >> .config
alias kmake='ARCH=arm CROSS_COMPILE=$ANDROID/prebuilts/gcc/linux-x86/arm/arm-linux-androideabi-4.6/bin/arm-linux-androideabi- make'
alias redir7000='echo "redir add tcp:7000:7000" | nc 127.0.0.1 5554 & PID=$!; sleep 1; kill $PID'
alias GENRAM='find . -print0 | cpio -o0 -H newc | gzip -9 > ../ramdisk.img'
