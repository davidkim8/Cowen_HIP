#!/opt/local/bin/perl -w

use strict;

open (IN,"gzcat simulated_reads2.fastq.gz |") || die "Can't open file!\n";
open (OUT,">simulated_reads2_filtered.fastq");

while (<IN>) {

   chomp;
   my $line2 = <IN>;
   my $line3 = <IN>;
   my $line4 = <IN>;

   if (length($line2) == length($line4)) {
      print OUT $_."\n".$line2.$line3.$line4;
   }

}

close(OUT);
close(IN);
